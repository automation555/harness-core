package io.harness.ng.core.invites.api.impl;

import static io.harness.ng.core.invites.InviteOperationResponse.ACCOUNT_INVITE_ACCEPTED;
import static io.harness.ng.core.invites.InviteOperationResponse.USER_ALREADY_ADDED;
import static io.harness.ng.core.invites.InviteOperationResponse.USER_INVITED_SUCCESSFULLY;
import static io.harness.ng.core.invites.InviteOperationResponse.USER_INVITE_RESENT;
import static io.harness.ng.core.invites.entities.Invite.InviteType.ADMIN_INITIATED_INVITE;
import static io.harness.rule.OwnerRule.ANKUSH;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.mongo.MongoConfig;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.JWTGeneratorUtils;
import io.harness.ng.core.invites.api.InviteService;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Invite.InviteKeys;
import io.harness.ng.core.invites.entities.UserMembership;
import io.harness.ng.core.user.User;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.repositories.invites.spring.InvitesRepository;
import io.harness.rule.Owner;

import com.auth0.jwt.interfaces.Claim;
import com.google.common.collect.ImmutableMap;
import com.mongodb.client.result.UpdateResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.support.TransactionTemplate;
import retrofit2.Response;

public class InvitesServiceImplTest extends CategoryTest {
  private final String accountIdentifier = randomAlphabetic(7);
  private final String orgIdentifier = randomAlphabetic(7);
  private final String projectIdentifier = randomAlphabetic(7);
  private final String emailId = String.format("%s@%s", randomAlphabetic(7), randomAlphabetic(7));
  private final String inviteId = randomAlphabetic(10);
  @Mock private JWTGeneratorUtils jwtGeneratorUtils;
  @Mock private NgUserService ngUserService;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private InvitesRepository invitesRepository;
  @Mock private NotificationClient notificationClient;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private AccessControlAdminClient accessControlAdminClient;
  private Invite invite;

  private InviteService inviteService;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    MongoConfig mongoConfig = MongoConfig.builder().uri("mongodb://localhost:27017/ng-harness").build();
    String baseUrl = "baseurl";
    String userVerificationSercret = "abcde";
    inviteService = new InviteServiceImpl(baseUrl, userVerificationSercret, mongoConfig, jwtGeneratorUtils,
        ngUserService, transactionTemplate, invitesRepository, notificationClient, "http://qa.harness.io",
        "http://qa.harness.io/ng/#", accessControlAdminClient);
    RoleAssignmentDTO roleAssignment =
        RoleAssignmentDTO.builder()
            .identifier("dummy")
            .principal(PrincipalDTO.builder().type(PrincipalType.USER).identifier(randomAlphabetic(10)).build())
            .resourceGroupIdentifier("dummyResourceGroup")
            .roleIdentifier("dummyRoleIdentifier")
            .build();
    List<RoleAssignmentDTO> roleAssignments = Collections.singletonList(roleAssignment);
    invite = Invite.builder()
                 .accountIdentifier(accountIdentifier)
                 .orgIdentifier(orgIdentifier)
                 .projectIdentifier(projectIdentifier)
                 .approved(Boolean.FALSE)
                 .email(emailId)
                 .name(randomAlphabetic(7))
                 .id(inviteId)
                 .inviteType(ADMIN_INITIATED_INVITE)
                 .roleAssignments(roleAssignments)
                 .build();
    RoleAssignmentResponseDTO roleAssignmentResponseDTO =
        RoleAssignmentResponseDTO.builder().roleAssignment(invite.getRoleAssignments().get(0)).build();
    when(accessControlAdminClient.createMultiRoleAssignment(any(), any(), any(), any()).execute())
        .thenReturn(Response.success(ResponseDTO.newResponse(Collections.singletonList(roleAssignmentResponseDTO))));
    when(accessControlAdminClient.deleteRoleAssignment(any(), any(), any(), any()).execute())
        .thenReturn(Response.success(ResponseDTO.newResponse(roleAssignmentResponseDTO)));
    when(accessControlAdminClient.updateRoleAssignment(any(), any(), any(), any(), any()).execute())
        .thenReturn(Response.success(ResponseDTO.newResponse(roleAssignmentResponseDTO)));
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_NullInvite() {
    InviteOperationResponse inviteOperationResponse = inviteService.create(null);
    assertThat(inviteOperationResponse).isEqualTo(InviteOperationResponse.FAIL);
  }

  private UserMembership getUserMembership(String userId) {
    return UserMembership.builder()
        .userId(userId)
        .scopes(Collections.singletonList(UserMembership.Scope.builder()
                                              .accountIdentifier(accountIdentifier)
                                              .orgIdentifier(orgIdentifier)
                                              .projectIdentifier(projectIdentifier)
                                              .build()))
        .build();
  }

  private UserMembership getUserMembershipWithNoScopes(String userId) {
    return UserMembership.builder().userId(userId).scopes(new ArrayList<>()).build();
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_UserAlreadyExists_UserAlreadyAdded() {
    String userId = randomAlphabetic(10);
    User user = User.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    UserMembership userMembership = getUserMembership(userId);
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.of(user));
    when(ngUserService.getUserMembership(any())).thenReturn(Optional.of(userMembership));
    InviteOperationResponse inviteOperationResponse = inviteService.create(invite);
    assertThat(inviteOperationResponse).isEqualTo(USER_ALREADY_ADDED);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_UserAlreadyExists_UserNotInvitedYet() {
    String userId = randomAlphabetic(10);
    User user = User.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    UserMembership userMembership = getUserMembershipWithNoScopes(userId);
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.of(user));
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserMembership(any())).thenReturn(Optional.of(userMembership));
    when(invitesRepository
             .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndInviteTypeAndDeleted(
                 any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());

    InviteOperationResponse inviteOperationResponse = inviteService.create(invite);

    assertThat(inviteOperationResponse).isEqualTo(USER_INVITED_SUCCESSFULLY);
    verify(invitesRepository, times(1))
        .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndInviteTypeAndDeleted(
            any(), any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_UserAlreadyExists_UserInvitedBefore() {
    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);

    String userId = randomAlphabetic(10);
    User user = User.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    UserMembership userMembership = getUserMembershipWithNoScopes(userId);
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.of(user));
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserMembership(any())).thenReturn(Optional.of(userMembership));
    when(invitesRepository
             .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndInviteTypeAndDeleted(
                 any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.of(invite));

    InviteOperationResponse inviteOperationResponse = inviteService.create(invite);

    assertThat(inviteOperationResponse).isEqualTo(USER_INVITE_RESENT);
    verify(invitesRepository, atLeast(2)).updateInvite(idArgumentCaptor.capture(), any());
    String id = idArgumentCaptor.getValue();
    assertThat(id).isEqualTo(inviteId);
    verify(notificationClient, times(1)).sendNotificationAsync(any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_NewUser_UserNotInvitedBefore() {
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.empty());
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserMembership(any())).thenReturn(Optional.empty());
    when(invitesRepository
             .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndInviteTypeAndDeleted(
                 any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());

    InviteOperationResponse inviteOperationResponse = inviteService.create(invite);

    assertThat(inviteOperationResponse).isEqualTo(USER_INVITED_SUCCESSFULLY);
    verify(notificationClient, times(1)).sendNotificationAsync(any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_NewUser_UserInvitedBefore() {
    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);

    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.empty());
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserMembership(any())).thenReturn(Optional.empty());
    when(invitesRepository
             .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndInviteTypeAndDeleted(
                 any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.of(invite));

    InviteOperationResponse inviteOperationResponse = inviteService.create(invite);

    assertThat(inviteOperationResponse).isEqualTo(USER_INVITE_RESENT);
    verify(invitesRepository, atLeast(2)).updateInvite(idArgumentCaptor.capture(), any());
    String id = idArgumentCaptor.getValue();
    assertThat(id).isEqualTo(inviteId);
    verify(notificationClient, times(1)).sendNotificationAsync(any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_NewUser_InviteAccepted() {
    invite.setApproved(Boolean.TRUE);
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.empty());
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserMembership(any())).thenReturn(Optional.empty());
    when(invitesRepository
             .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndInviteTypeAndDeleted(
                 any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.of(invite));

    InviteOperationResponse inviteOperationResponse = inviteService.create(invite);

    assertThat(inviteOperationResponse).isEqualTo(ACCOUNT_INVITE_ACCEPTED);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testUpdateInvite_ResendInvite() {
    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);

    when(invitesRepository.findDistinctByIdAndDeleted(eq(inviteId), any())).thenReturn(Optional.of(invite));
    when(invitesRepository.save(any())).thenReturn(invite);

    inviteService.updateInvite(invite);

    verify(invitesRepository, atLeast(2)).updateInvite(idArgumentCaptor.capture(), any());
    String id = idArgumentCaptor.getValue();
    assertThat(id).isEqualTo(inviteId);
    verify(notificationClient, times(1)).sendNotificationAsync(any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testDeleteInvite_InvitePresent() {
    ArgumentCaptor<Update> updateArgumentCaptor = ArgumentCaptor.forClass(Update.class);

    UpdateResult updateResult = mock(UpdateResult.class);
    when(updateResult.getModifiedCount()).thenReturn(1L);
    when(invitesRepository.save(any())).thenReturn(invite);
    when(invitesRepository.findDistinctByIdAndDeleted(eq(invite.getId()), any())).thenReturn(Optional.of(invite));
    when(invitesRepository.updateInvite(eq(inviteId), any())).thenReturn(updateResult);
    Optional<Invite> inviteOptional = inviteService.deleteInvite(inviteId);

    assertThat(inviteOptional.isPresent()).isTrue();
    Invite returnInvite = inviteOptional.get();
    assertThat(returnInvite.getId()).isEqualTo(invite.getId());
    verify(invitesRepository, times(1)).updateInvite(any(), updateArgumentCaptor.capture());
    Update update = updateArgumentCaptor.getValue();
    assertThat(update.getUpdateObject().size()).isEqualTo(1);
    assertThat(((Document) update.getUpdateObject().get("$set")).containsKey(InviteKeys.deleted)).isTrue();
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testDeleteInvite_InvalidInvite() {
    when(invitesRepository.findDistinctByIdAndDeleted(eq(invite.getId()), any())).thenReturn(Optional.empty());

    Optional<Invite> inviteOptional = inviteService.deleteInvite(inviteId);

    assertThat(inviteOptional.isPresent()).isFalse();
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testVerify_InvalidJWTToken() {
    inviteService.verify(randomAlphabetic(20));
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testVerify_ValidJWTToken_InviteIdDNE() {
    String jwtToken = randomAlphabetic(20);
    invite.setInviteToken(jwtToken);
    Claim idClaim = mock(Claim.class);
    String userId = randomAlphabetic(10);
    User user = User.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    when(idClaim.asString()).thenReturn(inviteId + "1", inviteId);
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.of(user));
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserMembership(any())).thenReturn(Optional.empty());
    when(invitesRepository.findDistinctByIdAndDeleted(any(), any())).thenReturn(Optional.empty(), Optional.of(invite));
    when(jwtGeneratorUtils.verifyJWTToken(any(), any())).thenReturn(ImmutableMap.of(InviteKeys.id, idClaim));

    Optional<Invite> inviteOptional = inviteService.verify(jwtToken);
    assertThat(inviteOptional.isPresent()).isFalse();

    inviteOptional = inviteService.verify(jwtToken);
    assertThat(inviteOptional.isPresent()).isTrue();
    assertThat(inviteOptional.get().getId()).isEqualTo(inviteId);
  }
}
