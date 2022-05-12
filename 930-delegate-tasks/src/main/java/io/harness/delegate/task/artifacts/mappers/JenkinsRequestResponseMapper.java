/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.mappers;

import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.jenkins.JenkinsUserNamePasswordDTO;
import io.harness.delegate.task.artifacts.jenkins.JenkinsArtifactDelegateRequest;
import io.harness.utils.FieldWithPlainTextOrSecretValueHelper;

import software.wings.beans.JenkinsConfig;

public class JenkinsRequestResponseMapper {
  public JenkinsConfig toJenkinsConfig(JenkinsArtifactDelegateRequest request) {
    String password = "";
    String username = "";
    if (request.getJenkinsConnectorDTO().getAuth() != null
        && request.getJenkinsConnectorDTO().getAuth().getCredentials() != null) {
      JenkinsUserNamePasswordDTO credentials =
          (JenkinsUserNamePasswordDTO) request.getJenkinsConnectorDTO().getAuth().getCredentials();
      if (credentials.getPasswordRef() != null) {
        password = EmptyPredicate.isNotEmpty(credentials.getPasswordRef().getDecryptedValue())
            ? new String(credentials.getPasswordRef().getDecryptedValue())
            : null;
      }
      username = FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef(
          credentials.getUsername(), credentials.getUsernameRef());
    }
    return JenkinsConfig.builder()
        .jenkinsUrl(request.getJenkinsConnectorDTO().getJenkinsUrl())
        .authMechanism(request.getJenkinsConnectorDTO().getAuth().getAuthType().getDisplayName())
        .username(username)
        .password(password.toCharArray())
        .build();
  }
}
