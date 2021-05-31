package io.harness.advisers.manualIntervention;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.contracts.execution.Status.INTERVENTION_WAITING;

import io.harness.advisers.CommonAdviserTypes;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.rollback.NGFailureActionTypeConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.advisers.InterventionWaitAdvise;
import io.harness.pms.contracts.commons.RepairActionCode;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.protobuf.Duration;
import java.util.Collections;
import java.util.Map;

@OwnedBy(HarnessTeam.CDC)
public class ManualInterventionAdviserWithRollback implements Adviser {
  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(CommonAdviserTypes.MANUAL_INTERVENTION_WITH_ROLLBACK.name()).build();

  @Inject private KryoSerializer kryoSerializer;

  @Override
  public AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent) {
    ManualInterventionAdviserRollbackParameters parameters = extractParameters(advisingEvent);
    Duration timeout = Duration.newBuilder().setSeconds(java.time.Duration.ofDays(1).toMinutes() * 60).build();
    if (parameters != null && parameters.getTimeout() != null) {
      timeout = Duration.newBuilder().setSeconds(parameters.getTimeout()).build();
    }
    RepairActionCode repairActionCode = parameters == null ? null : parameters.getTimeoutAction();
    return AdviserResponse.newBuilder()
        .setInterventionWaitAdvise(
            InterventionWaitAdvise.newBuilder()
                .setTimeout(timeout)
                .setRepairActionCode(
                    repairActionCode == null ? RepairActionCode.UNKNOWN : getReformedRepairActionCode(repairActionCode))
                .putAllMetadata(getRollbackMetadataMap(repairActionCode))
                .build())
        .setType(AdviseType.INTERVENTION_WAIT)
        .build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    if (checkIfPreviousAdviserExpired(advisingEvent.getNodeExecution())) {
      return false;
    }
    boolean canAdvise = StatusUtils.brokeStatuses().contains(advisingEvent.getToStatus())
        && advisingEvent.getFromStatus() != INTERVENTION_WAITING;
    ManualInterventionAdviserRollbackParameters parameters = extractParameters(advisingEvent);
    FailureInfo failureInfo = advisingEvent.getNodeExecution().getFailureInfo();
    if (failureInfo != null && parameters != null && !isEmpty(failureInfo.getFailureTypesList())) {
      return canAdvise
          && !Collections.disjoint(parameters.getApplicableFailureTypes(), failureInfo.getFailureTypesList());
    }
    return canAdvise;
  }

  private boolean checkIfPreviousAdviserExpired(NodeExecutionProto nodeExecutionProto) {
    if (nodeExecutionProto.getInterruptHistoriesCount() == 0) {
      return false;
    }
    return nodeExecutionProto.getInterruptHistoriesList()
        .get(nodeExecutionProto.getInterruptHistoriesCount() - 1)
        .getInterruptConfig()
        .getIssuedBy()
        .hasTimeoutIssuer();
  }

  private ManualInterventionAdviserRollbackParameters extractParameters(AdvisingEvent advisingEvent) {
    byte[] adviserParameters = advisingEvent.getAdviserParameters();
    if (isEmpty(adviserParameters)) {
      return null;
    }
    return (ManualInterventionAdviserRollbackParameters) kryoSerializer.asObject(adviserParameters);
  }

  private RepairActionCode getReformedRepairActionCode(RepairActionCode repairActionCode) {
    switch (repairActionCode) {
      case STAGE_ROLLBACK:
      case STEP_GROUP_ROLLBACK:
        return RepairActionCode.CUSTOM_FAILURE;
      default:
        return repairActionCode;
    }
  }

  private String getRollbackStrategy(RepairActionCode repairActionCode) {
    switch (repairActionCode) {
      case STEP_GROUP_ROLLBACK:
        return NGFailureActionTypeConstants.STEP_GROUP_ROLLBACK;
      case STAGE_ROLLBACK:
        return NGFailureActionTypeConstants.STAGE_ROLLBACK;
      default:
        return "";
    }
  }

  private Map<String, String> getRollbackMetadataMap(RepairActionCode repairActionCode) {
    String rollbackStrategy = getRollbackStrategy(repairActionCode);
    if (EmptyPredicate.isNotEmpty(rollbackStrategy)) {
      return Collections.singletonMap("ROLLBACK", rollbackStrategy);
    }
    return Collections.emptyMap();
  }
}
