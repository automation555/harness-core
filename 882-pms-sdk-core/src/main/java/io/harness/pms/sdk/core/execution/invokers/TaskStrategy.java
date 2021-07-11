package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.contracts.execution.Status.SKIPPED;
import static io.harness.pms.contracts.execution.Status.TASK_WAITING;
import static io.harness.pms.sdk.core.execution.invokers.StrategyHelper.buildResponseDataSupplier;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.SkipTaskExecutableResponse;
import io.harness.pms.contracts.execution.TaskExecutableResponse;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest.RequestCase;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.StringOutcome;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.ProgressableStrategy;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;

import com.google.inject.Inject;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(PIPELINE)
public class TaskStrategy extends ProgressableStrategy {
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private StrategyHelper strategyHelper;

  @Override
  public void start(InvokerPackage invokerPackage) {
    Ambiance ambiance = invokerPackage.getAmbiance();
    TaskExecutable taskExecutable = extractStep(ambiance);
    TaskRequest task =
        taskExecutable.obtainTask(ambiance, invokerPackage.getStepParameters(), invokerPackage.getInputPackage());
    handleResponse(ambiance, task);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    Ambiance ambiance = resumePackage.getAmbiance();
    TaskExecutable taskExecutable = extractStep(ambiance);
    StepResponse stepResponse = null;
    try {
      stepResponse = taskExecutable.handleTaskResult(
          ambiance, resumePackage.getStepParameters(), buildResponseDataSupplier(resumePackage.getResponseDataMap()));
    } catch (Exception e) {
      stepResponse = strategyHelper.handleException(e);
    }
    sdkNodeExecutionService.handleStepResponse(ambiance.getPlanExecutionId(),
        AmbianceUtils.obtainCurrentRuntimeId(ambiance), StepResponseMapper.toStepResponseProto(stepResponse));
  }

  private void handleResponse(@NonNull Ambiance ambiance, TaskRequest taskRequest) {
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    if (RequestCase.SKIPTASKREQUEST == taskRequest.getRequestCase()) {
      sdkNodeExecutionService.handleStepResponse(ambiance.getPlanExecutionId(), nodeExecutionId,
          StepResponseMapper.toStepResponseProto(
              StepResponse.builder()
                  .status(SKIPPED)
                  .stepOutcome(
                      StepOutcome.builder()
                          .name("skipOutcome")
                          .outcome(
                              StringOutcome.builder().message(taskRequest.getSkipTaskRequest().getMessage()).build())
                          .build())
                  .build()),
          ExecutableResponse.newBuilder()
              .setSkipTask(SkipTaskExecutableResponse.newBuilder()
                               .setMessage(taskRequest.getSkipTaskRequest().getMessage())
                               .build())
              .build());
      return;
    }

    ExecutableResponse executableResponse =
        ExecutableResponse.newBuilder()
            .setTask(
                TaskExecutableResponse.newBuilder()
                    .setTaskCategory(taskRequest.getTaskCategory())
                    .addAllLogKeys(CollectionUtils.emptyIfNull(taskRequest.getDelegateTaskRequest().getLogKeysList()))
                    .addAllUnits(CollectionUtils.emptyIfNull(taskRequest.getDelegateTaskRequest().getUnitsList()))
                    .setTaskName(taskRequest.getDelegateTaskRequest().getTaskName())
                    .build())
            .build();

    QueueTaskRequest queueTaskRequest = QueueTaskRequest.newBuilder()
                                            .putAllSetupAbstractions(ambiance.getSetupAbstractionsMap())
                                            .setTaskRequest(taskRequest)
                                            .setExecutableResponse(executableResponse)
                                            .setStatus(TASK_WAITING)
                                            .build();
    sdkNodeExecutionService.queueTaskRequest(ambiance.getPlanExecutionId(), nodeExecutionId, queueTaskRequest);
  }

  @Override
  public TaskExecutable extractStep(Ambiance ambiance) {
    return (TaskExecutable) stepRegistry.obtain(AmbianceUtils.getCurrentStepType(ambiance));
  }
}
