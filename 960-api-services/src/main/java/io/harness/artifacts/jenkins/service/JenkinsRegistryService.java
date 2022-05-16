/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.artifacts.jenkins.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.jenkins.beans.JenkinsInternalConfig;

import software.wings.helpers.ext.jenkins.JobDetails;

import java.net.URISyntaxException;
import java.util.List;

@OwnedBy(CDC)
public interface JenkinsRegistryService {
  /**
   * Validate the credentials
   *
   * @param jenkinsInternalConfig the jenkins config
   * @return boolean validate
   */
  boolean validateCredentials(JenkinsInternalConfig jenkinsInternalConfig);

  List<JobDetails> getJobs(JenkinsInternalConfig jenkinsInternalConfig);
}
