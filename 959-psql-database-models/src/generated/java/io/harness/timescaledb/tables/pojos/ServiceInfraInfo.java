/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables.pojos;

import java.io.Serializable;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class ServiceInfraInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  private String id;
  private String serviceName;
  private String serviceId;
  private String tag;
  private String envName;
  private String envId;
  private String envType;
  private String pipelineExecutionSummaryCdId;
  private String deploymentType;
  private String serviceStatus;
  private Long serviceStartts;
  private Long serviceEndts;
  private String accountid;
  private String orgidentifier;
  private String projectidentifier;
  private String artifactImage;

  public ServiceInfraInfo() {}

  public ServiceInfraInfo(ServiceInfraInfo value) {
    this.id = value.id;
    this.serviceName = value.serviceName;
    this.serviceId = value.serviceId;
    this.tag = value.tag;
    this.envName = value.envName;
    this.envId = value.envId;
    this.envType = value.envType;
    this.pipelineExecutionSummaryCdId = value.pipelineExecutionSummaryCdId;
    this.deploymentType = value.deploymentType;
    this.serviceStatus = value.serviceStatus;
    this.serviceStartts = value.serviceStartts;
    this.serviceEndts = value.serviceEndts;
    this.accountid = value.accountid;
    this.orgidentifier = value.orgidentifier;
    this.projectidentifier = value.projectidentifier;
    this.artifactImage = value.artifactImage;
  }

  public ServiceInfraInfo(String id, String serviceName, String serviceId, String tag, String envName, String envId,
      String envType, String pipelineExecutionSummaryCdId, String deploymentType, String serviceStatus,
      Long serviceStartts, Long serviceEndts, String accountid, String orgidentifier, String projectidentifier,
      String artifactImage) {
    this.id = id;
    this.serviceName = serviceName;
    this.serviceId = serviceId;
    this.tag = tag;
    this.envName = envName;
    this.envId = envId;
    this.envType = envType;
    this.pipelineExecutionSummaryCdId = pipelineExecutionSummaryCdId;
    this.deploymentType = deploymentType;
    this.serviceStatus = serviceStatus;
    this.serviceStartts = serviceStartts;
    this.serviceEndts = serviceEndts;
    this.accountid = accountid;
    this.orgidentifier = orgidentifier;
    this.projectidentifier = projectidentifier;
    this.artifactImage = artifactImage;
  }

  /**
   * Getter for <code>public.service_infra_info.id</code>.
   */
  public String getId() {
    return this.id;
  }

  /**
   * Setter for <code>public.service_infra_info.id</code>.
   */
  public ServiceInfraInfo setId(String id) {
    this.id = id;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.service_name</code>.
   */
  public String getServiceName() {
    return this.serviceName;
  }

  /**
   * Setter for <code>public.service_infra_info.service_name</code>.
   */
  public ServiceInfraInfo setServiceName(String serviceName) {
    this.serviceName = serviceName;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.service_id</code>.
   */
  public String getServiceId() {
    return this.serviceId;
  }

  /**
   * Setter for <code>public.service_infra_info.service_id</code>.
   */
  public ServiceInfraInfo setServiceId(String serviceId) {
    this.serviceId = serviceId;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.tag</code>.
   */
  public String getTag() {
    return this.tag;
  }

  /**
   * Setter for <code>public.service_infra_info.tag</code>.
   */
  public ServiceInfraInfo setTag(String tag) {
    this.tag = tag;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.env_name</code>.
   */
  public String getEnvName() {
    return this.envName;
  }

  /**
   * Setter for <code>public.service_infra_info.env_name</code>.
   */
  public ServiceInfraInfo setEnvName(String envName) {
    this.envName = envName;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.env_id</code>.
   */
  public String getEnvId() {
    return this.envId;
  }

  /**
   * Setter for <code>public.service_infra_info.env_id</code>.
   */
  public ServiceInfraInfo setEnvId(String envId) {
    this.envId = envId;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.env_type</code>.
   */
  public String getEnvType() {
    return this.envType;
  }

  /**
   * Setter for <code>public.service_infra_info.env_type</code>.
   */
  public ServiceInfraInfo setEnvType(String envType) {
    this.envType = envType;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.pipeline_execution_summary_cd_id</code>.
   */
  public String getPipelineExecutionSummaryCdId() {
    return this.pipelineExecutionSummaryCdId;
  }

  /**
   * Setter for <code>public.service_infra_info.pipeline_execution_summary_cd_id</code>.
   */
  public ServiceInfraInfo setPipelineExecutionSummaryCdId(String pipelineExecutionSummaryCdId) {
    this.pipelineExecutionSummaryCdId = pipelineExecutionSummaryCdId;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.deployment_type</code>.
   */
  public String getDeploymentType() {
    return this.deploymentType;
  }

  /**
   * Setter for <code>public.service_infra_info.deployment_type</code>.
   */
  public ServiceInfraInfo setDeploymentType(String deploymentType) {
    this.deploymentType = deploymentType;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.service_status</code>.
   */
  public String getServiceStatus() {
    return this.serviceStatus;
  }

  /**
   * Setter for <code>public.service_infra_info.service_status</code>.
   */
  public ServiceInfraInfo setServiceStatus(String serviceStatus) {
    this.serviceStatus = serviceStatus;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.service_startts</code>.
   */
  public Long getServiceStartts() {
    return this.serviceStartts;
  }

  /**
   * Setter for <code>public.service_infra_info.service_startts</code>.
   */
  public ServiceInfraInfo setServiceStartts(Long serviceStartts) {
    this.serviceStartts = serviceStartts;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.service_endts</code>.
   */
  public Long getServiceEndts() {
    return this.serviceEndts;
  }

  /**
   * Setter for <code>public.service_infra_info.service_endts</code>.
   */
  public ServiceInfraInfo setServiceEndts(Long serviceEndts) {
    this.serviceEndts = serviceEndts;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.accountid</code>.
   */
  public String getAccountid() {
    return this.accountid;
  }

  /**
   * Setter for <code>public.service_infra_info.accountid</code>.
   */
  public ServiceInfraInfo setAccountid(String accountid) {
    this.accountid = accountid;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.orgidentifier</code>.
   */
  public String getOrgidentifier() {
    return this.orgidentifier;
  }

  /**
   * Setter for <code>public.service_infra_info.orgidentifier</code>.
   */
  public ServiceInfraInfo setOrgidentifier(String orgidentifier) {
    this.orgidentifier = orgidentifier;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.projectidentifier</code>.
   */
  public String getProjectidentifier() {
    return this.projectidentifier;
  }

  /**
   * Setter for <code>public.service_infra_info.projectidentifier</code>.
   */
  public ServiceInfraInfo setProjectidentifier(String projectidentifier) {
    this.projectidentifier = projectidentifier;
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.artifact_image</code>.
   */
  public String getArtifactImage() {
    return this.artifactImage;
  }

  /**
   * Setter for <code>public.service_infra_info.artifact_image</code>.
   */
  public ServiceInfraInfo setArtifactImage(String artifactImage) {
    this.artifactImage = artifactImage;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final ServiceInfraInfo other = (ServiceInfraInfo) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (serviceName == null) {
      if (other.serviceName != null)
        return false;
    } else if (!serviceName.equals(other.serviceName))
      return false;
    if (serviceId == null) {
      if (other.serviceId != null)
        return false;
    } else if (!serviceId.equals(other.serviceId))
      return false;
    if (tag == null) {
      if (other.tag != null)
        return false;
    } else if (!tag.equals(other.tag))
      return false;
    if (envName == null) {
      if (other.envName != null)
        return false;
    } else if (!envName.equals(other.envName))
      return false;
    if (envId == null) {
      if (other.envId != null)
        return false;
    } else if (!envId.equals(other.envId))
      return false;
    if (envType == null) {
      if (other.envType != null)
        return false;
    } else if (!envType.equals(other.envType))
      return false;
    if (pipelineExecutionSummaryCdId == null) {
      if (other.pipelineExecutionSummaryCdId != null)
        return false;
    } else if (!pipelineExecutionSummaryCdId.equals(other.pipelineExecutionSummaryCdId))
      return false;
    if (deploymentType == null) {
      if (other.deploymentType != null)
        return false;
    } else if (!deploymentType.equals(other.deploymentType))
      return false;
    if (serviceStatus == null) {
      if (other.serviceStatus != null)
        return false;
    } else if (!serviceStatus.equals(other.serviceStatus))
      return false;
    if (serviceStartts == null) {
      if (other.serviceStartts != null)
        return false;
    } else if (!serviceStartts.equals(other.serviceStartts))
      return false;
    if (serviceEndts == null) {
      if (other.serviceEndts != null)
        return false;
    } else if (!serviceEndts.equals(other.serviceEndts))
      return false;
    if (accountid == null) {
      if (other.accountid != null)
        return false;
    } else if (!accountid.equals(other.accountid))
      return false;
    if (orgidentifier == null) {
      if (other.orgidentifier != null)
        return false;
    } else if (!orgidentifier.equals(other.orgidentifier))
      return false;
    if (projectidentifier == null) {
      if (other.projectidentifier != null)
        return false;
    } else if (!projectidentifier.equals(other.projectidentifier))
      return false;
    if (artifactImage == null) {
      if (other.artifactImage != null)
        return false;
    } else if (!artifactImage.equals(other.artifactImage))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
    result = prime * result + ((this.serviceName == null) ? 0 : this.serviceName.hashCode());
    result = prime * result + ((this.serviceId == null) ? 0 : this.serviceId.hashCode());
    result = prime * result + ((this.tag == null) ? 0 : this.tag.hashCode());
    result = prime * result + ((this.envName == null) ? 0 : this.envName.hashCode());
    result = prime * result + ((this.envId == null) ? 0 : this.envId.hashCode());
    result = prime * result + ((this.envType == null) ? 0 : this.envType.hashCode());
    result = prime * result
        + ((this.pipelineExecutionSummaryCdId == null) ? 0 : this.pipelineExecutionSummaryCdId.hashCode());
    result = prime * result + ((this.deploymentType == null) ? 0 : this.deploymentType.hashCode());
    result = prime * result + ((this.serviceStatus == null) ? 0 : this.serviceStatus.hashCode());
    result = prime * result + ((this.serviceStartts == null) ? 0 : this.serviceStartts.hashCode());
    result = prime * result + ((this.serviceEndts == null) ? 0 : this.serviceEndts.hashCode());
    result = prime * result + ((this.accountid == null) ? 0 : this.accountid.hashCode());
    result = prime * result + ((this.orgidentifier == null) ? 0 : this.orgidentifier.hashCode());
    result = prime * result + ((this.projectidentifier == null) ? 0 : this.projectidentifier.hashCode());
    result = prime * result + ((this.artifactImage == null) ? 0 : this.artifactImage.hashCode());
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ServiceInfraInfo (");

    sb.append(id);
    sb.append(", ").append(serviceName);
    sb.append(", ").append(serviceId);
    sb.append(", ").append(tag);
    sb.append(", ").append(envName);
    sb.append(", ").append(envId);
    sb.append(", ").append(envType);
    sb.append(", ").append(pipelineExecutionSummaryCdId);
    sb.append(", ").append(deploymentType);
    sb.append(", ").append(serviceStatus);
    sb.append(", ").append(serviceStartts);
    sb.append(", ").append(serviceEndts);
    sb.append(", ").append(accountid);
    sb.append(", ").append(orgidentifier);
    sb.append(", ").append(projectidentifier);
    sb.append(", ").append(artifactImage);

    sb.append(")");
    return sb.toString();
  }
}
