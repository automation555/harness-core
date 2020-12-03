package io.harness.facilitator.modes;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.io.StepParameters;

@OwnedBy(CDC)
@Redesign
public interface Abortable<T extends StepParameters, V> {
  void handleAbort(Ambiance ambiance, T stateParameters, V executableResponse);
}
