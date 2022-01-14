package io.harness.accesscontrol.roleassignments.privileged;

import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.aggregator.consumers.RoleAssignmentCRUDEventHandler;
import io.harness.aggregator.consumers.UserGroupCRUDEventHandler;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

@OwnedBy(HarnessTeam.PL)
@Singleton
@ValidateOnExecution
public class PrivilegedRoleAssignmentHandler implements RoleAssignmentCRUDEventHandler, UserGroupCRUDEventHandler {
  private final PrivilegedRoleAssignmentService privilegedRoleAssignmentService;

  @Inject
  public PrivilegedRoleAssignmentHandler(PrivilegedRoleAssignmentService privilegedRoleAssignmentService) {
    this.privilegedRoleAssignmentService = privilegedRoleAssignmentService;
  }

  @Override
  public void handleRoleAssignmentCreate(RoleAssignmentDBO roleAssignment) {
    // Do Nothing
  }

  @Override
  public void handleRoleAssignmentDelete(String id) {
    privilegedRoleAssignmentService.deleteByRoleAssignment(id);
  }

  @Override
  public void handleUserGroupCreate(UserGroupDBO userGroupDBO) {
    // Do nothing
  }

  @Override
  public void handleUserGroupUpdate(UserGroupDBO userGroupDBO) {
    // Do Nothing
  }

  @Override
  public void handleUserGroupDelete(String id) {
    // Do Nothing
  }
}
