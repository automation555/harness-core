package io.harness.gitsync.core.impl;

import static io.harness.gitsync.common.beans.BranchSyncStatus.SYNCED;
import static io.harness.gitsync.common.beans.BranchSyncStatus.SYNCING;
import static io.harness.gitsync.common.beans.BranchSyncStatus.UNSYNCED;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.common.beans.BranchSyncMetadata;
import io.harness.gitsync.common.beans.GitBranch;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepType;
import io.harness.gitsync.common.beans.YamlChangeSetEventType;
import io.harness.gitsync.common.beans.YamlChangeSetStatus;
import io.harness.gitsync.common.dtos.GitToHarnessProcessMsvcStepResponse;
import io.harness.gitsync.common.dtos.GitToHarnessProgressDTO;
import io.harness.gitsync.common.helper.GitToHarnessProgressHelper;
import io.harness.gitsync.common.service.GitBranchService;
import io.harness.gitsync.common.service.GitBranchSyncService;
import io.harness.gitsync.common.service.GitToHarnessProgressService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;
import io.harness.gitsync.core.service.YamlChangeSetHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class BranchSyncEventYamlChangeSetHandler implements YamlChangeSetHandler {
  private YamlGitConfigService yamlGitConfigService;
  private GitBranchSyncService gitBranchSyncService;
  private GitBranchService gitBranchService;
  private GitToHarnessProgressService gitToHarnessProgressService;
  private GitToHarnessProgressHelper gitToHarnessProgressHelper;

  @Override
  public YamlChangeSetStatus process(YamlChangeSetDTO yamlChangeSetDTO) {
    String accountIdentifier = yamlChangeSetDTO.getAccountId();
    String repoURL = yamlChangeSetDTO.getRepoUrl();
    String branch = yamlChangeSetDTO.getBranch();

    List<YamlGitConfigDTO> yamlGitConfigDTOList = yamlGitConfigService.getByRepo(repoURL);
    if (yamlGitConfigDTOList.isEmpty()) {
      log.info("Repo {} doesn't exist, ignoring the branch sync change set event : {}", repoURL, yamlChangeSetDTO);
      return YamlChangeSetStatus.SKIPPED;
    }

    YamlChangeSetStatus queueStatus =
        gitToHarnessProgressHelper.getQueueStatusIfEventInProgressOrAlreadyProcessed(yamlChangeSetDTO);
    if (queueStatus != null) {
      log.info("Ignoring event {} with queue status {} as event might be already completed or in process",
          yamlChangeSetDTO, queueStatus);
      return queueStatus;
    }

    GitBranch gitBranch = gitBranchService.get(accountIdentifier, repoURL, branch);
    if (gitBranch == null) {
      log.info("Branch sync not possible for branch {} in repo {} as no branch exists in the system", branch, repoURL);
      return YamlChangeSetStatus.SKIPPED;
    }
    if (gitBranch.getBranchSyncStatus() == UNSYNCED) {
      gitBranchService.updateBranchSyncStatus(accountIdentifier, repoURL, branch, SYNCING);
    } else if (gitBranch.getBranchSyncStatus() == SYNCED) {
      log.info("The branch sync for repoUrl [{}], branch [{}] has status [{}], hence skipping", repoURL, branch,
          gitBranch.getBranchSyncStatus());
      return YamlChangeSetStatus.SKIPPED;
    }

    // Init Progress Record for this event
    GitToHarnessProgressDTO gitToHarnessProgressRecord = gitToHarnessProgressService.initProgress(
        yamlChangeSetDTO, YamlChangeSetEventType.BRANCH_SYNC, GitToHarnessProcessingStepType.GET_FILES, null);

    BranchSyncMetadata branchSyncMetadata = (BranchSyncMetadata) yamlChangeSetDTO.getEventMetadata();
    try {
      log.info("Starting branch sync for the branch [{}]", branch);
      GitToHarnessProcessMsvcStepResponse gitToHarnessProcessMsvcStepResponse =
          gitBranchSyncService.processBranchSyncEvent(yamlGitConfigDTOList.get(0), yamlChangeSetDTO.getBranch(),
              yamlChangeSetDTO.getAccountId(), branchSyncMetadata.getFileToBeExcluded(),
              yamlChangeSetDTO.getChangesetId(), gitToHarnessProgressRecord.getUuid());
      if (gitToHarnessProcessMsvcStepResponse.getGitToHarnessProgressStatus().isSuccessStatus()) {
        gitBranchService.updateBranchSyncStatus(yamlChangeSetDTO.getAccountId(), repoURL, branch, SYNCED);
        log.info("Branch sync status updated completed for branch [{}]", branch);
        return YamlChangeSetStatus.COMPLETED;
      } else {
        gitBranchService.updateBranchSyncStatus(yamlChangeSetDTO.getAccountId(), repoURL, branch, SYNCED);
        log.error("G2H process files step failed with status : {}, marking branch sync event as FAILED for retry",
            gitToHarnessProcessMsvcStepResponse.getGitToHarnessProgressStatus());
        return YamlChangeSetStatus.FAILED_WITH_RETRY;
      }
    } catch (Exception ex) {
      log.error("Error encountered while syncing the branch [{}]", branch, ex);
      gitBranchService.updateBranchSyncStatus(yamlChangeSetDTO.getAccountId(), repoURL, branch, SYNCED);
      gitToHarnessProgressService.updateStepStatus(
          gitToHarnessProgressRecord.getUuid(), GitToHarnessProcessingStepStatus.ERROR);
      return YamlChangeSetStatus.FAILED_WITH_RETRY;
    }
  }
}
