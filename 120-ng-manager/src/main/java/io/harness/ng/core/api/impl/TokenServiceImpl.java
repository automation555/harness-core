package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.core.utils.NGUtils.validate;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.BCryptVersion.$2A;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.AccountOrgProjectValidator;
import io.harness.ng.core.api.ApiKeyService;
import io.harness.ng.core.api.TokenService;
import io.harness.ng.core.common.beans.ApiKeyType;
import io.harness.ng.core.dto.TokenAggregateDTO;
import io.harness.ng.core.dto.TokenDTO;
import io.harness.ng.core.dto.TokenFilterDTO;
import io.harness.ng.core.entities.ApiKey;
import io.harness.ng.core.entities.Token;
import io.harness.ng.core.entities.Token.TokenKeys;
import io.harness.ng.core.events.TokenCreateEvent;
import io.harness.ng.core.events.TokenDeleteEvent;
import io.harness.ng.core.events.TokenUpdateEvent;
import io.harness.ng.core.mapper.TokenDTOMapper;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.ng.core.spring.TokenRepository;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@OwnedBy(PL)
public class TokenServiceImpl implements TokenService {
  @Inject private TokenRepository tokenRepository;
  @Inject private ApiKeyService apiKeyService;
  @Inject private OutboxService outboxService;
  @Inject private AccountOrgProjectValidator accountOrgProjectValidator;
  @Inject @Named(OUTBOX_TRANSACTION_TEMPLATE) private TransactionTemplate transactionTemplate;
  @Inject private NgUserService ngUserService;

  private static final String deliminator = ".";

  @Override
  public String createToken(TokenDTO tokenDTO) {
    validateTokenRequest(tokenDTO.getAccountIdentifier(), tokenDTO.getOrgIdentifier(), tokenDTO.getProjectIdentifier(),
        tokenDTO.getApiKeyType(), tokenDTO.getParentIdentifier(), tokenDTO.getApiKeyIdentifier());
    String randomString = RandomStringUtils.random(20, 0, 0, true, true, null, new SecureRandom());
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder($2A, 10);
    String tokenString = passwordEncoder.encode(randomString);
    ApiKey apiKey = apiKeyService.getApiKey(tokenDTO.getAccountIdentifier(), tokenDTO.getOrgIdentifier(),
        tokenDTO.getProjectIdentifier(), tokenDTO.getApiKeyType(), tokenDTO.getParentIdentifier(),
        tokenDTO.getApiKeyIdentifier());
    tokenDTO.setIdentifier(tokenString);
    Token token = TokenDTOMapper.getTokenFromDTO(tokenDTO, apiKey.getDefaultTimeToExpireToken());
    validate(token);
    Token newToken = Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
      Token savedToken = tokenRepository.save(token);
      outboxService.save(new TokenCreateEvent(TokenDTOMapper.getDTOFromToken(savedToken)));
      return savedToken;
    }));
    return newToken.getUuid() + deliminator + randomString;
  }

  private void validateTokenRequest(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      ApiKeyType apiKeyType, String parentIdentifier, String apiKeyIdentifier) {
    if (!accountOrgProjectValidator.isPresent(accountIdentifier, orgIdentifier, projectIdentifier)) {
      throw new InvalidArgumentsException(String.format("Project [%s] in Org [%s] and Account [%s] does not exist",
                                              accountIdentifier, orgIdentifier, projectIdentifier),
          USER_SRE);
    }
    validateParentIdentifier(accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier);
    apiKeyService.getApiKey(
        accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier, apiKeyIdentifier);
  }

  private void validateParentIdentifier(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      ApiKeyType apiKeyType, String parentIdentifier) {
    switch (apiKeyType) {
      case USER:
        Optional<String> userId = Optional.empty();
        if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
            && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
          userId = Optional.of(SourcePrincipalContextBuilder.getSourcePrincipal().getName());
        }
        if (!userId.isPresent()) {
          throw new InvalidArgumentsException("No user identifier present in context");
        }
        if (!userId.get().equals(parentIdentifier)) {
          throw new InvalidArgumentsException(String.format(
              "User [%s] not authenticated to create api key for user [%s]", userId.get(), parentIdentifier));
        }
        break;
      case SERVICE_ACCOUNT:
        break;
      default:
        throw new InvalidArgumentsException(String.format("Invalid api key type: %s", apiKeyType));
    }
  }

  @Override
  public boolean revokeToken(String tokenIdentifier) {
    Optional<Token> optionalToken = tokenRepository.findByIdentifier(tokenIdentifier);
    Preconditions.checkState(optionalToken.isPresent(), "No token present with identifier: " + tokenIdentifier);
    return Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
      long deleted = tokenRepository.deleteByIdentifier(tokenIdentifier);
      if (deleted > 0) {
        outboxService.save(new TokenDeleteEvent(TokenDTOMapper.getDTOFromToken(optionalToken.get())));
        return true;
      } else {
        return false;
      }
    }));
  }

  @Override
  public TokenDTO getToken(String tokenId) {
    Optional<Token> optionalToken = tokenRepository.findById(tokenId);
    if (optionalToken.isPresent()) {
      TokenDTO tokenDTO = optionalToken.map(TokenDTOMapper::getDTOFromToken).orElse(null);
      if (ApiKeyType.USER == tokenDTO.getApiKeyType()) {
        Optional<UserInfo> optionalUserInfo = ngUserService.getUserById(tokenDTO.getParentIdentifier());
        if (optionalUserInfo.isPresent()) {
          UserInfo userInfo = optionalUserInfo.get();
          tokenDTO.setEmail(userInfo.getEmail());
          tokenDTO.setUsername(userInfo.getName());
          return tokenDTO;
        }
      } else {
        return tokenDTO;
      }
    }
    return null;
  }

  @Override
  public String rotateToken(String tokenIdentifier, Instant scheduledExpireTime) {
    Optional<Token> optionalToken = tokenRepository.findByIdentifier(tokenIdentifier);
    Preconditions.checkState(optionalToken.isPresent(), "No token present with identifier: " + tokenIdentifier);
    Token token = optionalToken.get();
    TokenDTO oldToken = TokenDTOMapper.getDTOFromToken(token);
    token.setScheduledExpireTime(scheduledExpireTime);
    token.setValidUntil(new Date(token.getExpiryTimestamp().toEpochMilli()));
    Token newToken = Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
      Token savedToken = tokenRepository.save(token);
      TokenDTO newTokenDTO = TokenDTOMapper.getDTOFromToken(savedToken);
      outboxService.save(new TokenUpdateEvent(oldToken, newTokenDTO));
      return savedToken;
    }));
    TokenDTO rotatedTokenDTO = TokenDTOMapper.getDTOFromTokenForRotation(newToken);
    return createToken(rotatedTokenDTO);
  }

  @Override
  public TokenDTO updateToken(TokenDTO tokenDTO) {
    validateTokenRequest(tokenDTO.getAccountIdentifier(), tokenDTO.getOrgIdentifier(), tokenDTO.getProjectIdentifier(),
        tokenDTO.getApiKeyType(), tokenDTO.getParentIdentifier(), tokenDTO.getApiKeyIdentifier());
    Optional<Token> optionalToken = tokenRepository.findByIdentifier(tokenDTO.getIdentifier());
    Preconditions.checkState(
        optionalToken.isPresent(), "No token present with identifier: " + tokenDTO.getIdentifier());
    Token token = optionalToken.get();
    TokenDTO oldToken = TokenDTOMapper.getDTOFromToken(token);
    token.setName(tokenDTO.getName());
    token.setValidFrom(Instant.ofEpochMilli(tokenDTO.getValidFrom()));
    token.setValidTo(Instant.ofEpochMilli(tokenDTO.getValidTo()));
    token.setValidUntil(new Date(token.getExpiryTimestamp().toEpochMilli()));
    validate(token);

    return Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
      Token savedToken = tokenRepository.save(token);
      TokenDTO newToken = TokenDTOMapper.getDTOFromToken(savedToken);
      outboxService.save(new TokenUpdateEvent(oldToken, newToken));
      return newToken;
    }));
  }

  @Override
  public Map<String, Integer> getTokensPerApiKeyIdentifier(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, ApiKeyType apiKeyType, String parentIdentifier, List<String> apiKeyIdentifiers) {
    return tokenRepository.getTokensPerParentIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier, apiKeyIdentifiers);
  }

  @Override
  public PageResponse<TokenAggregateDTO> listAggregateTokens(
      String accountIdentifier, Pageable pageable, TokenFilterDTO filterDTO) {
    Criteria criteria =
        createApiKeyFilterCriteria(Criteria.where(TokenKeys.accountIdentifier).is(accountIdentifier), filterDTO);
    Page<Token> tokens = tokenRepository.findAll(criteria, pageable);
    return PageUtils.getNGPageResponse(tokens.map(token -> {
      TokenDTO tokenDTO = TokenDTOMapper.getDTOFromToken(token);
      return TokenAggregateDTO.builder()
          .token(tokenDTO)
          .expiryAt(token.getExpiryTimestamp().toEpochMilli())
          .createdAt(token.getCreatedAt())
          .lastModifiedAt(token.getLastModifiedAt())
          .build();
    }));
  }

  private Criteria createApiKeyFilterCriteria(Criteria criteria, TokenFilterDTO filterDTO) {
    if (filterDTO == null) {
      return criteria;
    }
    if (isNotBlank(filterDTO.getSearchTerm())) {
      criteria.orOperator(Criteria.where(TokenKeys.name).regex(filterDTO.getSearchTerm(), "i"),
          Criteria.where(TokenKeys.identifier).regex(filterDTO.getSearchTerm(), "i"));
    }
    if (Objects.nonNull(filterDTO.getOrgIdentifier()) && !filterDTO.getOrgIdentifier().isEmpty()) {
      criteria.and(TokenKeys.orgIdentifier).is(filterDTO.getOrgIdentifier());
    }
    if (Objects.nonNull(filterDTO.getProjectIdentifier()) && !filterDTO.getProjectIdentifier().isEmpty()) {
      criteria.and(TokenKeys.projectIdentifier).is(filterDTO.getProjectIdentifier());
    }
    criteria.and(TokenKeys.apiKeyType).is(filterDTO.getApiKeyType());
    criteria.and(TokenKeys.parentIdentifier).is(filterDTO.getParentIdentifier());
    criteria.and(TokenKeys.apiKeyIdentifier).is(filterDTO.getApiKeyIdentifier());

    if (Objects.nonNull(filterDTO.getIdentifiers()) && !filterDTO.getIdentifiers().isEmpty()) {
      criteria.and(TokenKeys.identifier).in(filterDTO.getIdentifiers());
    }
    return criteria;
  }

  @Override
  public long deleteAllByParentIdentifier(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      ApiKeyType apiKeyType, String parentIdentifier) {
    return tokenRepository
        .deleteAllByAccountIdentifierAndOrgIdentifierAndParentIdentifierAndApiKeyTypeAndParentIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier);
  }

  @Override
  public long deleteAllByApiKeyIdentifier(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      ApiKeyType apiKeyType, String parentIdentifier, String apiKeyIdentifier) {
    return tokenRepository
        .deleteAllByAccountIdentifierAndOrgIdentifierAndParentIdentifierAndApiKeyTypeAndParentIdentifierAndApiKeyIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier, apiKeyIdentifier);
  }
}
