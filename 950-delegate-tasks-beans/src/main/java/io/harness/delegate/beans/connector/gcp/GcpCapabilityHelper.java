package io.harness.delegate.beans.connector.gcp;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class GcpCapabilityHelper extends ConnectorCapabilityBaseHelper {
  private static final String GCS_URL = "https://storage.googleapis.com/storage/";

  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ConnectorConfigDTO connectorConfigDTO, ExpressionEvaluator maskingEvaluator) {
    GcpConnectorDTO gcpConnectorDTO = (GcpConnectorDTO) connectorConfigDTO;
    GcpConnectorCredentialDTO credential = gcpConnectorDTO.getCredential();
    List<ExecutionCapability> capabilityList = new ArrayList<>();
    if (credential.getGcpCredentialType() == GcpCredentialType.MANUAL_CREDENTIALS
        || credential.getGcpCredentialType() == GcpCredentialType.INHERIT_FROM_DELEGATE) {
      capabilityList.add(
          HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(GCS_URL, maskingEvaluator));
    } else {
      throw new UnknownEnumTypeException("Gcp Credential Type", String.valueOf(credential.getGcpCredentialType()));
    }
    populateDelegateSelectorCapability(capabilityList, gcpConnectorDTO.getDelegateSelectors());
    return capabilityList;
  }
}
