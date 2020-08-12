package software.wings.resources.secretsmanagement;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.secretmanagerclient.NGConstants.ACCOUNT_KEY;
import static io.harness.secretmanagerclient.NGConstants.ORG_KEY;
import static io.harness.secretmanagerclient.NGConstants.PROJECT_KEY;

import com.google.inject.Inject;

import io.harness.NgManagerServiceDriver;
import io.harness.beans.PageResponse;
import io.harness.ng.core.NGAccessWithEncryptionConsumer;
import io.harness.rest.RestResponse;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.security.annotations.NextGenManagerAuth;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.NGSecretService;
import software.wings.settings.SettingVariableTypes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("secrets")
@Path("/ng/secrets")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
@Slf4j
public class SecretsResourceNG {
  private final NgManagerServiceDriver ngManagerServiceDriver;
  private final NGSecretService ngSecretService;
  public static final String LIMIT_KEY = "limit";
  public static final String OFFSET_KEY = "offset";

  @Inject
  public SecretsResourceNG(NgManagerServiceDriver ngManagerServiceDriver, NGSecretService ngSecretService) {
    this.ngManagerServiceDriver = ngManagerServiceDriver;
    this.ngSecretService = ngSecretService;
  }

  private PageResponse<EncryptedDataDTO> getPageResponse(PageResponse<EncryptedData> encryptedDataPageResponse) {
    List<EncryptedDataDTO> dtoList =
        encryptedDataPageResponse.getResponse().stream().map(EncryptedDataMapper::toDTO).collect(Collectors.toList());
    PageResponse<EncryptedDataDTO> dtoPageResponse = new PageResponse<>();
    dtoPageResponse.setResponse(dtoList);
    dtoPageResponse.setTotal(encryptedDataPageResponse.getTotal());
    dtoPageResponse.setLimit(encryptedDataPageResponse.getLimit());
    dtoPageResponse.setOffset(encryptedDataPageResponse.getOffset());
    return dtoPageResponse;
  }

  @POST
  @Produces("application/x-kryo")
  @Consumes("application/x-kryo")
  public RestResponse<EncryptedDataDTO> createSecret(SecretTextDTO dto) {
    EncryptedData createdEncryptedData = ngSecretService.createSecretText(dto);
    return new RestResponse<>(EncryptedDataMapper.toDTO(createdEncryptedData));
  }

  @GET
  public RestResponse<PageResponse<EncryptedDataDTO>> listSecrets(
      @QueryParam(ACCOUNT_KEY) final String accountIdentifier, @QueryParam(ORG_KEY) final String orgIdentifier,
      @QueryParam(PROJECT_KEY) final String projectIdentifier,
      @QueryParam(LIMIT_KEY) @DefaultValue("100") final String limit,
      @QueryParam(OFFSET_KEY) @DefaultValue("0") final String offset, @QueryParam("searchTerm") final String searchTerm,
      @QueryParam("type") final SettingVariableTypes type) {
    PageResponse<EncryptedData> encryptedDataPageResponse;
    if (!StringUtils.isEmpty(searchTerm)) {
      List<EncryptedData> encryptedDataList =
          ngSecretService.searchSecrets(accountIdentifier, orgIdentifier, projectIdentifier, type, searchTerm);
      encryptedDataPageResponse =
          aPageResponse().withResponse(encryptedDataList).withTotal(encryptedDataList.size()).build();
      return new RestResponse<>(getPageResponse(encryptedDataPageResponse));
    }
    encryptedDataPageResponse =
        ngSecretService.listSecrets(accountIdentifier, orgIdentifier, projectIdentifier, type, limit, offset);
    return new RestResponse<>(getPageResponse(encryptedDataPageResponse));
  }

  @GET
  @Path("{identifier}")
  public RestResponse<EncryptedDataDTO> get(@PathParam("identifier") String identifier,
      @QueryParam(ACCOUNT_KEY) final String accountIdentifier, @QueryParam(ORG_KEY) final String orgIdentifier,
      @QueryParam(PROJECT_KEY) final String projectIdentifier) {
    Optional<EncryptedData> encryptedDataOptional =
        ngSecretService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return new RestResponse<>(encryptedDataOptional.map(EncryptedDataMapper::toDTO).orElse(null));
  }

  @PUT
  @Path("{identifier}")
  @Consumes({"application/x-kryo"})
  public RestResponse<Boolean> updateSecret(@PathParam("identifier") String identifier,
      @QueryParam(ACCOUNT_KEY) final String account, @QueryParam(ORG_KEY) final String org,
      @QueryParam(PROJECT_KEY) final String project, SecretTextUpdateDTO dto) {
    return new RestResponse<>(ngSecretService.updateSecretText(account, org, project, identifier, dto));
  }

  @DELETE
  @Path("{identifier}")
  public RestResponse<Boolean> deleteSecret(@PathParam("identifier") String identifier,
      @QueryParam(ACCOUNT_KEY) final String accountIdentifier, @QueryParam(ORG_KEY) final String orgIdentifier,
      @QueryParam(PROJECT_KEY) final String projectIdentifier) {
    return new RestResponse<>(
        ngSecretService.deleteSecretText(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }

  @POST
  @Path("encryption-details")
  @Consumes("application/x-kryo")
  @Produces("application/x-kryo")
  public RestResponse<List<EncryptedDataDetail>> getEncryptionDetails(
      NGAccessWithEncryptionConsumer ngAccessWithEncryptionConsumer) {
    return new RestResponse<>(ngSecretService.getEncryptionDetails(
        ngAccessWithEncryptionConsumer.getNgAccess(), ngAccessWithEncryptionConsumer.getDecryptableEntity()));
  }

  @GET
  @Path("task")
  public RestResponse<Boolean> sendTaskResponse() {
    boolean sendTaskResultResponse = ngManagerServiceDriver.sendTaskResult(generateUuid(), null);
    return new RestResponse<>(sendTaskResultResponse);
  }
}
