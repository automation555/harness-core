package io.harness.gitsync.fullsync.dtos;

import io.harness.NGCommonEntityConstants;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(name = "GitFullSyncConfig", description = "This has config details specific to Git Full Sync with Harness")
public class GitFullSyncConfigDTO {
  @Schema(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) private String accountIdentifier;
  @Schema(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) private String orgIdentifier;
  @Schema(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) private String projectIdentifier;
  @Schema(description = "Source Branch for pull request") private String baseBranch;
  @Schema(description = "Branch on which Entities will be pushed") private String branch;
  @Schema(description = "PR Title") private String message;
  @Schema(description = "This checks whether to create a pull request. Its default value is False")
  private boolean createPullRequest;
  @Schema(description = GitSyncApiConstants.REPOID_PARAM_MESSAGE) private String repoIdentifier;
}
