package software.wings.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import software.wings.api.ExecutionDataValue;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.sm.StateExecutionData;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@FieldNameConstants(innerTypeName = "VerificationStateAnalysisExecutionDataKeys")
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = false)
public class VerificationStateAnalysisExecutionData extends StateExecutionData {
  @JsonIgnore @Inject private WingsPersistence wingsPersistence;

  private String correlationId;
  private String stateExecutionInstanceId;
  private String baselineExecutionId;
  private String serverConfigId;
  private Set<String> canaryNewHostNames;
  private Set<String> lastExecutionNodes;
  private int analysisMinute;
  private String query;
  private int progressPercentage;
  private AnalysisComparisonStrategy comparisonStrategy;
  private long remainingMinutes;

  @Override
  @JsonIgnore
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = getExecutionDetails();
    putNotNull(executionDetails, VerificationStateAnalysisExecutionDataKeys.stateExecutionInstanceId,
        ExecutionDataValue.builder().displayName("State Execution Id").value(stateExecutionInstanceId).build());
    return executionDetails;
  }

  @Override
  @JsonIgnore
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(
        executionDetails, "errorMsg", ExecutionDataValue.builder().displayName("Message").value(getErrorMsg()).build());
    removeCrypticHostNames(canaryNewHostNames);
    removeCrypticHostNames(lastExecutionNodes);

    putNotNull(executionDetails, "query", ExecutionDataValue.builder().displayName("Query").value(query).build());
    putNotNull(executionDetails, "newVersionNodes",
        ExecutionDataValue.builder().displayName("New version nodes").value(canaryNewHostNames).build());
    putNotNull(executionDetails, "previousVersionNodes",
        ExecutionDataValue.builder().displayName("Previous version nodes").value(lastExecutionNodes).build());
    return executionDetails;
  }

  private void removeCrypticHostNames(Set<String> hostNames) {
    if (isEmpty(hostNames)) {
      return;
    }

    for (Iterator<String> iterator = hostNames.iterator(); iterator.hasNext();) {
      final String hostName = iterator.next();
      if (hostName.startsWith("testNode") || hostName.startsWith("controlNode")) {
        iterator.remove();
      }
    }
  }
}
