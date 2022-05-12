/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.artifacts.jenkins.service;

import io.harness.artifacts.jenkins.beans.JenkinsInternalConfig;

import software.wings.beans.JenkinsConfig;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;

import com.google.inject.Inject;

public class JenkinsRegistryUtils {
  public static final String TOKEN_FIELD = "Bearer Token(HTTP Header)";

  @Inject private JenkinsFactory jenkinsFactory;

  public Jenkins getJenkins(JenkinsInternalConfig jenkinsInternalConfig) {
    if (TOKEN_FIELD.equals(jenkinsInternalConfig.getAuthMechanism())) {
      return jenkinsFactory.create(jenkinsInternalConfig.getJenkinsUrl(), jenkinsInternalConfig.getToken());
    } else {
      return jenkinsFactory.create(jenkinsInternalConfig.getJenkinsUrl(), jenkinsInternalConfig.getUsername(),
          jenkinsInternalConfig.getPassword());
    }
  }
}
