/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.approval.step.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "HarnessApprovalActivityKeys")
public class HarnessApprovalActivity {
  @NotNull EmbeddedUser user;
  @NotNull HarnessApprovalAction action;
  List<ApproverInput> approverInputs;
  String comments;
  long approvedAt;

  public static HarnessApprovalActivity fromHarnessApprovalActivityRequestDTO(
      EmbeddedUser user, HarnessApprovalActivityRequestDTO request) {
    if (user == null || request == null) {
      return null;
    }

    return HarnessApprovalActivity.builder()
        .user(user)
        .action(request.getAction())
        .approverInputs(request.getApproverInputs())
        .comments(request.getComments())
        .approvedAt(System.currentTimeMillis())
        .build();
  }
}
