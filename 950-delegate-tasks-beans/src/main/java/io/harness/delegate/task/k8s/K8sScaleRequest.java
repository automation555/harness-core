package io.harness.delegate.task.k8s;

import static io.harness.expression.Expression.DISALLOW_SECRETS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.NGInstanceUnitType;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.expression.Expression;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CDP)
public class K8sScaleRequest implements K8sDeployRequest {
  @Expression(DISALLOW_SECRETS) String releaseName;
  String commandName;
  K8sTaskType taskType;
  Integer timeoutIntervalInMin;
  K8sInfraDelegateConfig k8sInfraDelegateConfig;
  ManifestDelegateConfig manifestDelegateConfig;
  String workload;
  Integer instances;
  NGInstanceUnitType instanceUnitType;
  Optional<Integer> maxInstances;
  boolean skipSteadyStateCheck;
  CommandUnitsProgress commandUnitsProgress;
  boolean useVarSupportForKustomize;
  boolean useNewKubectlVersion;

  @Override
  public List<String> getValuesYamlList() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getKustomizePatchesList() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getOpenshiftParamList() {
    return Collections.emptyList();
  }
}
