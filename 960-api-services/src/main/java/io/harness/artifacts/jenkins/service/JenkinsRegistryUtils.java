/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.artifacts.jenkins.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.threading.Morpheus.quietSleep;
import static io.harness.threading.Morpheus.sleep;

import static java.time.Duration.ofMillis;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.artifacts.jenkins.beans.JenkinsInternalConfig;
import io.harness.artifacts.jenkins.client.JenkinsClient;
import io.harness.artifacts.jenkins.client.JenkinsCustomServer;
import io.harness.concurrent.HTimeLimiter;
import io.harness.exception.ArtifactServerException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;

import software.wings.helpers.ext.jenkins.CustomJenkinsHttpClient;
import software.wings.helpers.ext.jenkins.JobDetails;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.Job;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpResponseException;

@Slf4j
public class JenkinsRegistryUtils {
  public static final String TOKEN_FIELD = "Bearer Token(HTTP Header)";
  public static final String USERNAME_PASSWORD = "UsernamePassword";
  private final String FOLDER_JOB_CLASS_NAME = "com.cloudbees.hudson.plugins.folder.Folder";
  private final String MULTI_BRANCH_JOB_CLASS_NAME =
      "org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject";
  private final String BLUE_STEEL_TEAM_FOLDER_CLASS_NAME =
      "com.cloudbees.opscenter.bluesteel.folder.BlueSteelTeamFolder";
  private final String ORGANIZATION_FOLDER_CLASS_NAME = "jenkins.branch.OrganizationFolder";
  private final String SERVER_ERROR = "Server Error";

  @Inject private ExecutorService executorService;
  @Inject private TimeLimiter timeLimiter;

  public boolean isRunning(JenkinsInternalConfig jenkinsInternalConfig) {
    try {
      CustomJenkinsHttpClient client = JenkinsClient.getJenkinsHttpClient(jenkinsInternalConfig);
      client.get("/");
      return true;
    } catch (IOException | URISyntaxException e) {
      throw prepareWingsException((IOException) e);
    }
  }

  public List<JobDetails> getJobs(JenkinsInternalConfig jenkinsInternalConfig, String parentJob) {
    try {
      return HTimeLimiter.callInterruptible21(timeLimiter, Duration.ofSeconds(120), () -> {
        while (true) {
          List<JobDetails> details = getJobDetails(jenkinsInternalConfig, parentJob);
          if (details != null) {
            return details;
          }
          sleep(ofMillis(100L));
        }
      });
    } catch (Exception e) {
      throw new ArtifactServerException(ExceptionUtils.getMessage(e), e, USER);
    }
  }

  private List<JobDetails> getJobDetails(JenkinsInternalConfig jenkinsInternalConfig, String parentJob) {
    List<JobDetails> result = new ArrayList<>(); // TODO:: extend jobDetails to keep track of prefix.
    try {
      JenkinsCustomServer jenkinsServer = JenkinsClient.getJenkinsServer(jenkinsInternalConfig);
      Stack<Job> jobs = new Stack<>();
      Queue<Future> futures = new ConcurrentLinkedQueue<>();
      if (isBlank(parentJob)) {
        return jenkinsServer.getJobs()
            .values()
            .stream()
            .map(job -> new JobDetails(getJobNameFromUrl(job.getUrl()), job.getUrl(), isFolderJob(job)))
            .collect(toList());
      } else {
        jobs.addAll(jenkinsServer.getJobs(new FolderJob(parentJob, "/job/" + parentJob + "/")).values());
      }

      while (!jobs.empty() || !futures.isEmpty()) {
        while (!jobs.empty()) {
          Job job = jobs.pop();
          if (isFolderJob(job)) {
            futures.add(executorService.submit(
                () -> jobs.addAll(jenkinsServer.getJobs(new FolderJob(job.getName(), job.getUrl())).values())));
          } else {
            String jobName = getJobNameFromUrl(job.getUrl());
            result.add(new JobDetails(jobName, job.getUrl(), false));
          }
        }
        while (!futures.isEmpty() && futures.peek().isDone()) {
          futures.poll().get();
        }
        quietSleep(ofMillis(10));
      }
      return result;
    } catch (Exception ex) {
      log.error("Error in fetching job lists ", ex);
      return result;
    }
  }

  private String getJobNameFromUrl(String url) {
    // TODO:: remove it post review. Extend jobDetails object
    // Each jenkins server could have a different base url.
    // Whichever is the format, the url after the base would always start with "/job/"
    String relativeUrl;
    String pattern = ".*?/job/";

    relativeUrl = url.replaceFirst(pattern, "");

    String[] parts = relativeUrl.split("/");
    StringBuilder nameBuilder = new StringBuilder();
    // We start with index 0 since /job/ has already been
    for (int idx = 0; idx <= parts.length - 1; idx = idx + 2) {
      nameBuilder.append('/').append(parts[idx]);
    }
    String name = nameBuilder.toString();
    name = name.charAt(0) == '/' ? name.substring(1) : name;
    return getNormalizedName(name);
  }

  protected String getNormalizedName(String jobName) {
    try {
      if (isNotEmpty(jobName)) {
        return URLDecoder.decode(jobName, Charsets.UTF_8.name());
      }
    } catch (UnsupportedEncodingException e) {
      log.warn("Failed to decode jobName {}", jobName, e);
    }
    return jobName;
  }

  private boolean isFolderJob(Job job) {
    // job.get_class().equals(FOLDER_JOB_CLASS_NAME) is to find if the jenkins job is of type folder.
    // (job instanceOf FolderJob) doesn't work
    return job.get_class().equals(FOLDER_JOB_CLASS_NAME) || job.get_class().equals(MULTI_BRANCH_JOB_CLASS_NAME)
        || job.get_class().equals(BLUE_STEEL_TEAM_FOLDER_CLASS_NAME)
        || job.get_class().equals(ORGANIZATION_FOLDER_CLASS_NAME);
  }

  private WingsException prepareWingsException(IOException e) {
    if (e instanceof HttpResponseException) {
      if (((HttpResponseException) e).getStatusCode() == 401) {
        throw new ArtifactServerException("Invalid Jenkins credentials", USER);
      } else if (((HttpResponseException) e).getStatusCode() == 403) {
        throw new ArtifactServerException("User not authorized to access jenkins", USER);
      }
    }
    throw new ArtifactServerException(ExceptionUtils.getMessage(e), e, USER);
  }
}
