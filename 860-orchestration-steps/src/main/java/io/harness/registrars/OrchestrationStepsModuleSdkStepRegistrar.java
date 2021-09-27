package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.steps.NGSpecStep;
import io.harness.steps.StagesStep;
import io.harness.steps.common.NGExecutionStep;
import io.harness.steps.common.NGSectionStep;
import io.harness.steps.common.NGSectionStepWithRollbackInfo;
import io.harness.steps.common.steps.stepgroup.StepGroupStep;
import io.harness.steps.fork.NGForkStep;
import io.harness.steps.section.chain.SectionChainStep;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

// TODO (prashant) : Merge with OrchestrationStepsModuleStepRegistrar
@OwnedBy(CDC)
@UtilityClass
public class OrchestrationStepsModuleSdkStepRegistrar {
  public Map<StepType, Class<? extends Step>> getEngineSteps() {
    Map<StepType, Class<? extends Step>> engineSteps = new HashMap<>();

    engineSteps.put(SectionChainStep.STEP_TYPE, SectionChainStep.class);
    engineSteps.put(StepGroupStep.STEP_TYPE, StepGroupStep.class);
    engineSteps.put(NGForkStep.STEP_TYPE, NGForkStep.class);
    engineSteps.put(NGSectionStep.STEP_TYPE, NGSectionStep.class);
    engineSteps.put(NGSectionStepWithRollbackInfo.STEP_TYPE, NGSectionStepWithRollbackInfo.class);
    engineSteps.put(NGExecutionStep.STEP_TYPE, NGExecutionStep.class);
    engineSteps.put(NGSpecStep.STEP_TYPE, NGSpecStep.class);
    engineSteps.put(StagesStep.STEP_TYPE, StagesStep.class);

    return engineSteps;
  }
}
