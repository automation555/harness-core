package io.harness.states.codebase;

import io.harness.beans.execution.ExecutionSource;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CodeBaseStepParameters implements StepParameters {
  String codeBaseSyncTaskId;
  String codeBaseDelegateTaskId;

  String connectorRef;
  String repoUrl;
  ExecutionSource executionSource;
}
