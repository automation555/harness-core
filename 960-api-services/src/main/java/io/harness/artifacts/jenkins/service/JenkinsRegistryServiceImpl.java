/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.artifacts.jenkins.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.connectableJenkinsHttpUrl;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.jenkins.beans.JenkinsInternalConfig;
import io.harness.exception.ArtifactServerException;

import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.service.impl.jenkins.JenkinsUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class JenkinsRegistryServiceImpl implements JenkinsRegistryService {
  @Inject private JenkinsRegistryUtils jenkinsRegistryUtils;

  @Override
  public boolean validateCredentials(JenkinsInternalConfig jenkinsInternalConfig) {
    if (JenkinsUtils.TOKEN_FIELD.equals(jenkinsInternalConfig.getAuthMechanism())) {
      if (isEmpty(new String(jenkinsInternalConfig.getToken()))) {
        throw new ArtifactServerException("Token should be not empty", USER);
      }
    } else {
      if (isEmpty(jenkinsInternalConfig.getUsername()) || isEmpty(new String(jenkinsInternalConfig.getPassword()))) {
        throw new ArtifactServerException("UserName/Password should be not empty", USER);
      }
    }

    if (!connectableJenkinsHttpUrl(jenkinsInternalConfig.getJenkinsUrl())) {
      throw new ArtifactServerException(
          "Could not reach Jenkins Server at : " + jenkinsInternalConfig.getJenkinsUrl(), USER);
    }

    Jenkins jenkins = jenkinsRegistryUtils.getJenkins(jenkinsInternalConfig);

    return jenkins.isRunning();
  }
}
