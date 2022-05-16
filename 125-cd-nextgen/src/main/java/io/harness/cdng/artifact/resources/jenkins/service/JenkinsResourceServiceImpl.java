/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.resources.jenkins.service;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;

import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.resources.jenkins.dtos.JenkinsResponseDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.jenkins.JenkinsConnectorDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class JenkinsResourceServiceImpl implements JenkinsResourceService {
  private final ConnectorService connectorService;
  private final SecretManagerClientService secretManagerClientService;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @VisibleForTesting static final int timeoutInSecs = 30;

  @Inject
  public JenkinsResourceServiceImpl(@Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService connectorService,
      SecretManagerClientService secretManagerClientService) {
    this.connectorService = connectorService;
    this.secretManagerClientService = secretManagerClientService;
  }

  @Override
  public JenkinsResponseDTO getJobDetails(
      IdentifierRef jenkinsConnectorRef, String orgIdentifier, String projectIdentifier) {
    JenkinsConnectorDTO connector = getConnector(jenkinsConnectorRef);
    BaseNGAccess baseNGAccess =
        getBaseNGAccess(jenkinsConnectorRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptionDetails = getEncryptionDetails(connector, baseNGAccess);
    return null;
  }

  private List<EncryptedDataDetail> getEncryptionDetails(
      @Nonnull JenkinsConnectorDTO jenkinsConnectorDTO, @Nonnull NGAccess ngAccess) {
    if (jenkinsConnectorDTO.getAuth() != null && jenkinsConnectorDTO.getAuth().getCredentials() != null) {
      return secretManagerClientService.getEncryptionDetails(ngAccess, jenkinsConnectorDTO.getAuth().getCredentials());
    }
    return new ArrayList<>();
  }

  private JenkinsConnectorDTO getConnector(IdentifierRef jenkinsConnectorRef) {
    Optional<ConnectorResponseDTO> connectorDTO =
        connectorService.get(jenkinsConnectorRef.getAccountIdentifier(), jenkinsConnectorRef.getOrgIdentifier(),
            jenkinsConnectorRef.getProjectIdentifier(), jenkinsConnectorRef.getIdentifier());

    if (!connectorDTO.isPresent() || !isAJenkinsConnector(connectorDTO.get())) {
      throw new InvalidRequestException(String.format("Connector not found for identifier : [%s] with scope: [%s]",
                                            jenkinsConnectorRef.getIdentifier(), jenkinsConnectorRef.getScope()),
          WingsException.USER);
    }
    ConnectorInfoDTO connectors = connectorDTO.get().getConnector();
    return (JenkinsConnectorDTO) connectors.getConnectorConfig();
  }

  private boolean isAJenkinsConnector(@Valid @NotNull ConnectorResponseDTO connectorResponseDTO) {
    return ConnectorType.JENKINS == (connectorResponseDTO.getConnector().getConnectorType());
  }

  private BaseNGAccess getBaseNGAccess(String accountId, String orgIdentifier, String projectIdentifier) {
    return BaseNGAccess.builder()
        .accountIdentifier(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }
}
