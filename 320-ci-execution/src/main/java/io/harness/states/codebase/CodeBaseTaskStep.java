package io.harness.states.codebase;

import static io.harness.beans.execution.ExecutionSource.Type.MANUAL;
import static io.harness.beans.execution.ExecutionSource.Type.WEBHOOK;
import static io.harness.beans.sweepingoutputs.CISweepingOutputNames.CODEBASE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.TaskType.SCM_GIT_REF_TASK;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.execution.BranchWebhookEvent;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.execution.ManualExecutionSource;
import io.harness.beans.execution.PRWebhookEvent;
import io.harness.beans.execution.WebhookEvent;
import io.harness.beans.execution.WebhookExecutionSource;
import io.harness.beans.sweepingoutputs.CodebaseSweepingOutput;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.task.scm.GitRefType;
import io.harness.delegate.task.scm.ScmGitRefTaskParams;
import io.harness.delegate.task.scm.ScmGitRefTaskResponseData;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.product.ci.scm.proto.Commit;
import io.harness.product.ci.scm.proto.FindPRResponse;
import io.harness.product.ci.scm.proto.GetLatestCommitResponse;
import io.harness.product.ci.scm.proto.ListCommitsInPRResponse;
import io.harness.product.ci.scm.proto.PullRequest;
import io.harness.serializer.KryoSerializer;
import io.harness.stateutils.buildstate.ConnectorUtils;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CodeBaseTaskStep implements TaskExecutable<CodeBaseTaskStepParameters, ScmGitRefTaskResponseData>,
                                         SyncExecutable<CodeBaseTaskStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType("CI_CODEBASE_TASK").setStepCategory(StepCategory.STEP).build();

  @Inject private KryoSerializer kryoSerializer;
  @Inject private ConnectorUtils connectorUtils;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;

  @Override
  public Class<CodeBaseTaskStepParameters> getStepParametersClass() {
    return CodeBaseTaskStepParameters.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, CodeBaseTaskStepParameters stepParameters, StepInputPackage inputPackage) {
    ExecutionSource executionSource = stepParameters.getExecutionSource();
    if (executionSource.getType() != MANUAL) {
      throw new CIStageExecutionException("{} type is not supported in codebase delegate task for scm api operation");
    }

    ManualExecutionSource manualExecutionSource = (ManualExecutionSource) executionSource;
    ConnectorDetails connectorDetails =
        connectorUtils.getConnectorDetails(AmbianceUtils.getNgAccess(ambiance), stepParameters.getConnectorRef());

    ScmGitRefTaskParams scmGitRefTaskParams =
        obtainTaskParameters(manualExecutionSource, connectorDetails, stepParameters.getRepoUrl());

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(Duration.ofSeconds(30).toMillis())
                                  .taskType(SCM_GIT_REF_TASK.name())
                                  .parameters(new Object[] {scmGitRefTaskParams})
                                  .build();

    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer);
  }

  private ScmGitRefTaskParams obtainTaskParameters(
      ManualExecutionSource manualExecutionSource, ConnectorDetails connectorDetails, String repoName) {
    ScmConnector scmConnector = (ScmConnector) connectorDetails.getConnectorConfig();
    String completeUrl = scmConnector.getUrl();
    if (isNotEmpty(repoName)) {
      completeUrl = StringUtils.stripEnd(scmConnector.getUrl(), "/") + "/" + StringUtils.stripStart(repoName, "/");
    }
    scmConnector.setUrl(completeUrl);

    String branch = manualExecutionSource.getBranch();
    String prNumber = manualExecutionSource.getPrNumber();
    if (isNotEmpty(branch)) {
      return ScmGitRefTaskParams.builder()
          .branch(branch)
          .gitRefType(GitRefType.LATEST_COMMIT_ID)
          .encryptedDataDetails(connectorDetails.getEncryptedDataDetails())
          .scmConnector(scmConnector)
          .build();
    } else if (isNotEmpty(prNumber)) {
      return ScmGitRefTaskParams.builder()
          .prNumber(Long.parseLong(prNumber))
          .gitRefType(GitRefType.PULL_REQUEST_WITH_COMMITS)
          .encryptedDataDetails(connectorDetails.getEncryptedDataDetails())
          .scmConnector((ScmConnector) connectorDetails.getConnectorConfig())
          .build();
    } else {
      throw new CIStageExecutionException("Manual codebase git task needs at least PR number or branch");
    }
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, CodeBaseTaskStepParameters stepParameters,
      ThrowingSupplier<ScmGitRefTaskResponseData> responseDataSupplier) throws Exception {
    ScmGitRefTaskResponseData scmGitRefTaskResponseData = responseDataSupplier.get();
    CodebaseSweepingOutput codebaseSweepingOutput = null;

    if (scmGitRefTaskResponseData.getGitRefType() == GitRefType.PULL_REQUEST_WITH_COMMITS) {
      final byte[] findPRResponseByteArray = scmGitRefTaskResponseData.getFindPRResponse();
      final byte[] listCommitsInPRResponseByteArray = scmGitRefTaskResponseData.getListCommitsInPRResponse();

      if (findPRResponseByteArray == null || listCommitsInPRResponseByteArray == null) {
        throw new CIStageExecutionException("Codebase git information can't be obtained");
      }

      FindPRResponse findPRResponse = FindPRResponse.parseFrom(findPRResponseByteArray);
      ListCommitsInPRResponse listCommitsInPRResponse =
          ListCommitsInPRResponse.parseFrom(listCommitsInPRResponseByteArray);
      PullRequest pr = findPRResponse.getPr();
      List<Commit> commits = listCommitsInPRResponse.getCommitsList();
      List<CodebaseSweepingOutput.CodeBaseCommit> codeBaseCommits = new ArrayList<>();
      for (Commit commit : commits) {
        codeBaseCommits.add(CodebaseSweepingOutput.CodeBaseCommit.builder()
                                .id(commit.getSha())
                                .message(commit.getMessage())
                                .link(commit.getLink())
                                .timeStamp(commit.getCommitter().getDate().getSeconds())
                                .ownerEmail(commit.getAuthor().getEmail())
                                .ownerId(commit.getAuthor().getLogin())
                                .ownerName(commit.getAuthor().getName())
                                .build());
      }

      String state = "open";
      if (pr.getClosed()) {
        state = "closed";
      } else if (pr.getMerged()) {
        state = "merged";
      }
      codebaseSweepingOutput =
          CodebaseSweepingOutput.builder()
              .branch(pr.getTarget())
              .sourceBranch(pr.getSource())
              .targetBranch(pr.getTarget())
              .prNumber(String.valueOf(pr.getNumber()))
              .prTitle(pr.getTitle())
              .commitSha(pr.getSha())
              .baseCommitSha(pr.getBase().getSha())
              .commitRef(pr.getRef())
              .repoUrl(stepParameters.getRepoUrl()) // Add repo url to scm.PullRequest and get it from there
              .gitUser(pr.getAuthor().getName())
              .gitUserAvatar(pr.getAuthor().getAvatar())
              .gitUserEmail(pr.getAuthor().getEmail())
              .gitUserId(pr.getAuthor().getLogin())
              .pullRequestLink(pr.getLink())
              .commits(codeBaseCommits)
              .state(state)
              .build();

    } else if (scmGitRefTaskResponseData.getGitRefType() == GitRefType.LATEST_COMMIT_ID) {
      final byte[] getLatestCommitResponseByteArray = scmGitRefTaskResponseData.getGetLatestCommitResponse();
      if (isEmpty(getLatestCommitResponseByteArray)) {
        throw new CIStageExecutionException("Codebase git information can't be obtained");
      }
      GetLatestCommitResponse getLatestCommitResponse =
          GetLatestCommitResponse.parseFrom(getLatestCommitResponseByteArray);

      codebaseSweepingOutput = CodebaseSweepingOutput.builder()
                                   .branch(scmGitRefTaskResponseData.getBranch())
                                   .commitSha(getLatestCommitResponse.getCommitId())
                                   .build();
    }

    saveCodebaseSweepingOutput(ambiance, codebaseSweepingOutput);
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  private void saveCodebaseSweepingOutput(Ambiance ambiance, CodebaseSweepingOutput codebaseSweepingOutput) {
    OptionalSweepingOutput optionalSweepingOutput =
        executionSweepingOutputResolver.resolveOptional(ambiance, RefObjectUtils.getOutcomeRefObject(CODEBASE));
    if (!optionalSweepingOutput.isFound()) {
      try {
        executionSweepingOutputResolver.consume(
            ambiance, CODEBASE, codebaseSweepingOutput, StepOutcomeGroup.PIPELINE.name());
      } catch (Exception e) {
        log.error("Error while consuming codebase sweeping output", e);
      }
    }
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, CodeBaseTaskStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    ExecutionSource executionSource = stepParameters.getExecutionSource();

    CodebaseSweepingOutput codebaseSweepingOutput = null;
    if (executionSource.getType() == MANUAL) {
      ManualExecutionSource manualExecutionSource = (ManualExecutionSource) executionSource;
      codebaseSweepingOutput = CodebaseSweepingOutput.builder()
                                   .branch(manualExecutionSource.getBranch())
                                   .tag(manualExecutionSource.getTag())
                                   .commitSha(manualExecutionSource.getCommitSha())
                                   .build();
    } else if (executionSource.getType() == WEBHOOK) {
      WebhookExecutionSource webhookExecutionSource = (WebhookExecutionSource) executionSource;
      if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.PR) {
        PRWebhookEvent prWebhookEvent = (PRWebhookEvent) webhookExecutionSource.getWebhookEvent();

        codebaseSweepingOutput = CodebaseSweepingOutput.builder()
                                     .branch(prWebhookEvent.getSourceBranch())
                                     .targetBranch(prWebhookEvent.getTargetBranch())
                                     .sourceBranch(prWebhookEvent.getSourceBranch())
                                     .prNumber(String.valueOf(prWebhookEvent.getPullRequestId()))
                                     .prTitle(prWebhookEvent.getTitle())
                                     .commitSha(prWebhookEvent.getBaseAttributes().getAfter())
                                     .baseCommitSha(prWebhookEvent.getBaseAttributes().getBefore())
                                     .repoUrl(prWebhookEvent.getRepository().getLink())
                                     .gitUser(prWebhookEvent.getBaseAttributes().getAuthorName())
                                     .gitUserEmail(prWebhookEvent.getBaseAttributes().getAuthorEmail())
                                     .gitUserAvatar(prWebhookEvent.getBaseAttributes().getAuthorAvatar())
                                     .gitUserAvatar(prWebhookEvent.getBaseAttributes().getAuthorLogin())
                                     .build();
      } else if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.BRANCH) {
        BranchWebhookEvent branchWebhookEvent = (BranchWebhookEvent) webhookExecutionSource.getWebhookEvent();
        codebaseSweepingOutput = CodebaseSweepingOutput.builder()
                                     .branch(branchWebhookEvent.getBranchName())
                                     .targetBranch(branchWebhookEvent.getBranchName())
                                     .commitSha(branchWebhookEvent.getBaseAttributes().getAfter())
                                     .repoUrl(branchWebhookEvent.getRepository().getLink())
                                     .gitUser(branchWebhookEvent.getBaseAttributes().getAuthorName())
                                     .gitUserEmail(branchWebhookEvent.getBaseAttributes().getAuthorEmail())
                                     .gitUserAvatar(branchWebhookEvent.getBaseAttributes().getAuthorAvatar())
                                     .gitUserAvatar(branchWebhookEvent.getBaseAttributes().getAuthorLogin())
                                     .build();
      }
    }
    saveCodebaseSweepingOutput(ambiance, codebaseSweepingOutput);

    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
