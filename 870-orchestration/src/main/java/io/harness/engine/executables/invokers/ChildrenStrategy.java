package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executables.ExecuteStrategy;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executables.ResumePackage;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ChildrenExecutableResponse;
import io.harness.pms.execution.ChildrenExecutableResponse.Child;
import io.harness.pms.execution.ExecutableResponse;
import io.harness.pms.execution.NodeExecutionProto;
import io.harness.pms.execution.Status;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.LevelUtils;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.steps.executables.ChildrenExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.registries.StepRegistry;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
@Redesign
public class ChildrenStrategy implements ExecuteStrategy {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private OrchestrationEngine engine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    ChildrenExecutable childrenExecutable = extractChildrenExecutable(nodeExecution);
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildrenExecutableResponse response = childrenExecutable.obtainChildren(
        ambiance, nodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(nodeExecution, invokerPackage.getNodes(), response);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecutionProto nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildrenExecutable childrenExecutable = extractChildrenExecutable(nodeExecution);
    StepResponse stepResponse = childrenExecutable.handleChildrenResponse(ambiance,
        nodeExecutionService.extractResolvedStepParameters(nodeExecution), resumePackage.getResponseDataMap());
    engine.handleStepResponse(nodeExecution.getUuid(), stepResponse);
  }

  private ChildrenExecutable extractChildrenExecutable(NodeExecutionProto nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (ChildrenExecutable) stepRegistry.obtain(node.getStepType());
  }

  private void handleResponse(
      NodeExecutionProto nodeExecution, List<PlanNodeProto> nodes, ChildrenExecutableResponse response) {
    Ambiance ambiance = nodeExecution.getAmbiance();
    List<String> callbackIds = new ArrayList<>();
    for (Child child : response.getChildrenList()) {
      String uuid = generateUuid();
      callbackIds.add(uuid);
      PlanNodeProto node = findNode(nodes, child.getChildNodeId());
      Ambiance clonedAmbiance = AmbianceUtils.cloneForChild(ambiance, LevelUtils.buildLevelFromPlanNode(uuid, node));
      NodeExecutionProto childNodeExecution = NodeExecutionProto.newBuilder()
                                                  .setUuid(uuid)
                                                  .setNode(node)
                                                  .setAmbiance(clonedAmbiance)
                                                  .setStatus(Status.QUEUED)
                                                  .setNotifyId(uuid)
                                                  .setParentId(nodeExecution.getUuid())
                                                  .build();
      nodeExecutionService.save(childNodeExecution);
      executorService.submit(
          ExecutionEngineDispatcher.builder().ambiance(clonedAmbiance).orchestrationEngine(engine).build());
    }
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, callbackIds.toArray(new String[0]));
    nodeExecutionService.update(nodeExecution.getUuid(),
        ops
        -> ops.addToSet(
            NodeExecutionKeys.executableResponses, ExecutableResponse.newBuilder().setChildren(response).build()));
  }
}
