/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.jenkins;

import static io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper.populateDelegateSelectorCapability;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.jenkins.JenkinsConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.task.artifacts.ArtifactSourceDelegateRequest;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.helpers.ext.jenkins.JobDetails;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@OwnedBy(HarnessTeam.PIPELINE)
public class JenkinsArtifactDelegateRequest implements ArtifactSourceDelegateRequest {
  /** Images in repos need to be referenced via a path. */
  String imagePath;
  /** Tag refers to exact tag number. */
  String tag;
  /** Tag regex is used to get latest build from builds matching regex. */
  String tagRegex;
  /** List of buildNumbers/tags */
  List<String> tagsList;
  String connectorRef;
  List<JobDetails> jobDetails;
  /** DockerHub Connector*/
  JenkinsConnectorDTO jenkinsConnectorDTO;
  /** Encrypted details for decrypting.*/
  List<EncryptedDataDetail> encryptedDataDetails;
  /** Artifact Source type.*/
  ArtifactSourceType sourceType;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilities =
        new ArrayList<>(EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
            encryptedDataDetails, maskingEvaluator));
    populateDelegateSelectorCapability(capabilities, jenkinsConnectorDTO.getDelegateSelectors());
    capabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        jenkinsConnectorDTO.getJenkinsUrl().endsWith("/") ? jenkinsConnectorDTO.getJenkinsUrl()
                                                          : jenkinsConnectorDTO.getJenkinsUrl().concat("/"),
        maskingEvaluator));
    return capabilities;
  }
}
