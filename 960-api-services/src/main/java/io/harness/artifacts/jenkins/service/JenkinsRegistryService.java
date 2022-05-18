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

import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;

import com.offbytwo.jenkins.model.JobWithDetails;
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

  /**
   * Get the Jobs for the Jenkins
   *
   * @param jenkinsInternalConfig the jenkins config
   * @param parentJobName for the folder
   * @return List JobDetails
   */
  List<JobDetails> getJobs(JenkinsInternalConfig jenkinsInternalConfig, String parentJobName);

  /**
   * Get the Artifact Paths for the Jenkins
   *
   * @param jenkinsInternalConfig the jenkins config
   * @param jobName for the job
   * @return JobWithDetails
   */
  JobWithDetails getJobWithDetails(JenkinsInternalConfig jenkinsInternalConfig, String jobName);

  /**
   * Get the Artifact Paths for the Jenkins
   *
   * @param jenkinsInternalConfig the jenkins config
   * @param jobName for the job
   * @param artifactPaths
   * @param lastN for the build limit
   * @return List BuildDetails
   */
  List<BuildDetails> getBuildsForJob(
      JenkinsInternalConfig jenkinsInternalConfig, String jobName, List<String> artifactPaths, int lastN);
}
