package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.BarrierInstance;
import software.wings.beans.OrchestrationWorkflow;

import java.util.List;
import javax.validation.Valid;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface BarrierService {
  String save(@Valid BarrierInstance barrier);
  BarrierInstance get(String barrierId);
  BarrierInstance update(String appId, String barrierId);
  BarrierInstance update(BarrierInstance barrierInstance);

  String findByStep(String appId, String pipelineStageId, int pipelineStageParallelIndex, String workflowExecutionId,
      String identifier);

  @Value
  @Builder
  class OrchestrationWorkflowInfo {
    private String pipelineStageId;
    private String workflowId;
    private boolean isLooped;
    private OrchestrationWorkflow orchestrationWorkflow;
  }

  List<BarrierInstance> obtainInstances(
      String appId, List<OrchestrationWorkflowInfo> orchestrations, String pipelineExecutionId, int parallelIndex);

  void updateAllActiveBarriers(String appId);
}
