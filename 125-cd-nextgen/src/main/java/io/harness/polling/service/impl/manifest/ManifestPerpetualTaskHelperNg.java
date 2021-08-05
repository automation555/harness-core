package io.harness.polling.service.impl.manifest;

import static io.harness.delegate.task.helm.HelmValuesFetchRequest.getHelmExecutionCapabilities;
import static io.harness.utils.DelegateOwner.getNGTaskSetupAbstractionsWithOwner;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.delegate.Capability;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.k8s.HelmChartManifestDelegateConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.polling.ManifestCollectionTaskParamsNg;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.polling.bean.PollingDocument;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@OwnedBy(HarnessTeam.CDC)
public class ManifestPerpetualTaskHelperNg {
  K8sStepHelper k8sStepHelper;
  KryoSerializer kryoSerializer;

  public PerpetualTaskExecutionBundle createPerpetualTaskExecutionBundle(PollingDocument pollingDocument) {
    Any perpetualTaskParams;
    List<ExecutionCapability> executionCapabilities;
    ManifestOutcome manifestOutcome = (ManifestOutcome) pollingDocument.getPollingInfo();
    String accountId = pollingDocument.getAccountId();
    Ambiance ambiance = Ambiance.newBuilder()
                            .putSetupAbstractions("accountId", pollingDocument.getAccountId())
                            .putSetupAbstractions("orgIdentifier", pollingDocument.getOrgIdentifier())
                            .putSetupAbstractions("projectIdentifier", pollingDocument.getProjectIdentifier())
                            .build();

    final Map<String, String> ngTaskSetupAbstractionsWithOwner = getNGTaskSetupAbstractionsWithOwner(
        accountId, pollingDocument.getOrgIdentifier(), pollingDocument.getProjectIdentifier());

    if (ManifestType.HelmChart.equals(manifestOutcome.getType())) {
      HelmChartManifestDelegateConfig helmManifest =
          (HelmChartManifestDelegateConfig) k8sStepHelper.getManifestDelegateConfig(manifestOutcome, ambiance);
      executionCapabilities =
          getHelmExecutionCapabilities(helmManifest.getHelmVersion(), helmManifest.getStoreDelegateConfig(), null);
      ManifestCollectionTaskParamsNg manifestCollectionTaskParamsNg =
          ManifestCollectionTaskParamsNg.newBuilder()
              .setAccountId(accountId)
              .setPollingDocId(pollingDocument.getUuid())
              .setManifestCollectionParams(ByteString.copyFrom(kryoSerializer.asBytes(helmManifest)))
              .build();
      perpetualTaskParams = Any.pack(manifestCollectionTaskParamsNg);
    } else {
      throw new InvalidRequestException(String.format("Invalid type %s for polling", manifestOutcome.getType()));
    }

    PerpetualTaskExecutionBundle.Builder builder = PerpetualTaskExecutionBundle.newBuilder();
    executionCapabilities.forEach(executionCapability
        -> builder
               .addCapabilities(
                   Capability.newBuilder()
                       .setKryoCapability(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(executionCapability)))
                       .build())
               .build());
    return builder.setTaskParams(perpetualTaskParams).putAllSetupAbstractions(ngTaskSetupAbstractionsWithOwner).build();
  }
}
