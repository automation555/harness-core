package io.harness.cdng.service.dashboard;

import static io.harness.ng.core.activityhistory.dto.TimeGroupType.DAY;
import static io.harness.ng.core.activityhistory.dto.TimeGroupType.HOUR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.Deployment.DashboardDeploymentActiveFailedRunningInfo;
import io.harness.cdng.Deployment.DashboardWorkloadDeployment;
import io.harness.cdng.Deployment.Deployment;
import io.harness.cdng.Deployment.DeploymentCount;
import io.harness.cdng.Deployment.DeploymentDateAndCount;
import io.harness.cdng.Deployment.DeploymentInfo;
import io.harness.cdng.Deployment.DeploymentStatusInfo;
import io.harness.cdng.Deployment.DeploymentStatusInfoList;
import io.harness.cdng.Deployment.ExecutionDeployment;
import io.harness.cdng.Deployment.ExecutionDeploymentDetailInfo;
import io.harness.cdng.Deployment.ExecutionDeploymentInfo;
import io.harness.cdng.Deployment.HealthDeploymentDashboard;
import io.harness.cdng.Deployment.HealthDeploymentInfo;
import io.harness.cdng.Deployment.ServiceDeploymentInfo;
import io.harness.cdng.Deployment.TimeAndStatusDeployment;
import io.harness.cdng.Deployment.TotalDeploymentInfo;
import io.harness.cdng.Deployment.WorkloadCountInfo;
import io.harness.cdng.Deployment.WorkloadDateCountInfo;
import io.harness.cdng.Deployment.WorkloadDeploymentInfo;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.activityhistory.dto.TimeGroupType;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Singleton
@Slf4j
public class CDOverviewDashboardServiceImpl implements CDOverviewDashboardService {
  @Inject TimeScaleDBService timeScaleDBService;

  private String tableNameCD = "pipeline_execution_summary_cd";
  private String tableNameServiceAndInfra = "service_infra_info";
  private List<String> failedStatusList =
      Arrays.asList(ExecutionStatus.FAILED.name(), ExecutionStatus.ABORTED.name(), ExecutionStatus.EXPIRED.name());
  private List<String> activeStatusList = Arrays.asList(ExecutionStatus.RUNNING.name(), ExecutionStatus.PAUSED.name());
  private List<String> pendingStatusList = Arrays.asList(ExecutionStatus.INTERVENTION_WAITING.name(),
      ExecutionStatus.APPROVAL_WAITING.name(), ExecutionStatus.WAITING.name());
  private static final int MAX_RETRY_COUNT = 5;

  private static final long HOUR_IN_MS = 60 * 60 * 1000;
  private static final long DAY_IN_MS = 24 * HOUR_IN_MS;

  public String queryBuilderSelectStatusTime(
      String accountId, String orgId, String projectId, long startInterval, long endInterval) {
    String selectStatusQuery = "select status,startts from " + tableNameCD + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(selectStatusQuery);

    if (accountId != null) {
      totalBuildSqlBuilder.append(String.format("accountid='%s' and ", accountId));
    }

    if (orgId != null) {
      totalBuildSqlBuilder.append(String.format("orgidentifier='%s' and ", orgId));
    }

    if (projectId != null) {
      totalBuildSqlBuilder.append(String.format("projectidentifier='%s' and ", projectId));
    }

    if (startInterval > 0 && endInterval > 0) {
      totalBuildSqlBuilder.append(String.format("startts>=%s and startts<%s;", startInterval, endInterval));
    }

    return totalBuildSqlBuilder.toString();
  }

  public String queryBuilderSelectIdCdTable(
      String accountId, String orgId, String projectId, long startInterval, long endInterval) {
    String selectStatusQuery = "select id from " + tableNameCD + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(selectStatusQuery);

    if (accountId != null) {
      totalBuildSqlBuilder.append(String.format("accountid='%s' and ", accountId));
    }

    if (orgId != null) {
      totalBuildSqlBuilder.append(String.format("orgidentifier='%s' and ", orgId));
    }

    if (projectId != null) {
      totalBuildSqlBuilder.append(String.format("projectidentifier='%s' and ", projectId));
    }

    if (startInterval > 0 && endInterval > 0) {
      totalBuildSqlBuilder.append(String.format("startts>=%s and startts<%s;", startInterval, endInterval));
    }

    return totalBuildSqlBuilder.toString();
  }

  public String queryBuilderSelectIdLimitTimeCdTable(
      String accountId, String orgId, String projectId, long days, List<String> statusList) {
    String selectStatusQuery = "select id from " + tableNameCD + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(selectStatusQuery);

    if (accountId != null) {
      totalBuildSqlBuilder.append(String.format("accountid='%s' and ", accountId));
    }

    if (orgId != null) {
      totalBuildSqlBuilder.append(String.format("orgidentifier='%s' and ", orgId));
    }

    if (projectId != null) {
      totalBuildSqlBuilder.append(String.format("projectidentifier='%s' and ", projectId));
    }

    totalBuildSqlBuilder.append("status in (");
    for (String status : statusList) {
      totalBuildSqlBuilder.append(String.format("'%s',", status));
    }

    totalBuildSqlBuilder.deleteCharAt(totalBuildSqlBuilder.length() - 1);

    totalBuildSqlBuilder.append(String.format(") ORDER BY startts DESC LIMIT %s", days));

    return totalBuildSqlBuilder.toString();
  }

  public String queryBuilderEnvironmentType(
      String accountId, String orgId, String projectId, long startInterval, long endInterval) {
    String selectStatusQuery = "select env_type from " + tableNameCD + ", " + tableNameServiceAndInfra + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(selectStatusQuery);

    if (accountId != null) {
      totalBuildSqlBuilder.append(String.format("accountid='%s' and ", accountId));
    }

    if (orgId != null) {
      totalBuildSqlBuilder.append(String.format("orgidentifier='%s' and ", orgId));
    }

    if (projectId != null) {
      totalBuildSqlBuilder.append(String.format("projectidentifier='%s' and ", projectId));
    }

    if (startInterval > 0 && endInterval > 0) {
      totalBuildSqlBuilder.append(
          "pipeline_execution_summary_cd.id=pipeline_execution_summary_cd_id and env_type is not null and ");
      totalBuildSqlBuilder.append(String.format("startts>=%s and startts<%s;", startInterval, endInterval));
    }

    return totalBuildSqlBuilder.toString();
  }
  public double getRate(long current, long previous) {
    double rate = 0.0;
    if (previous != 0) {
      rate = (current - previous) / (double) previous;
    }
    rate = rate * 100.0;
    return rate;
  }

  public String queryBuilderStatus(
      String accountId, String orgId, String projectId, long days, List<String> statusList) {
    String selectStatusQuery = "select id,name,startts,endTs,status from " + tableNameCD + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(selectStatusQuery);

    if (accountId != null) {
      totalBuildSqlBuilder.append(String.format("accountid='%s' and ", accountId));
    }

    if (orgId != null) {
      totalBuildSqlBuilder.append(String.format("orgidentifier='%s' and ", orgId));
    }

    if (projectId != null) {
      totalBuildSqlBuilder.append(String.format("projectidentifier='%s' and ", projectId));
    }

    totalBuildSqlBuilder.append("status in (");
    for (String status : statusList) {
      totalBuildSqlBuilder.append(String.format("'%s',", status));
    }

    totalBuildSqlBuilder.deleteCharAt(totalBuildSqlBuilder.length() - 1);

    totalBuildSqlBuilder.append(String.format(") ORDER BY startts DESC LIMIT %s;", days));

    return totalBuildSqlBuilder.toString();
  }

  public String queryBuilderServiceTag(String queryIdCdTable) {
    String selectStatusQuery =
        "select service_name,tag,pipeline_execution_summary_cd_id from " + tableNameServiceAndInfra + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder(20480);

    totalBuildSqlBuilder.append(String.format(
        selectStatusQuery + "pipeline_execution_summary_cd_id in (%s) and service_name is not null;", queryIdCdTable));

    return totalBuildSqlBuilder.toString();
  }

  public String queryBuilderSelectWorkload(
      String accountId, String orgId, String projectId, long previousStartInterval, long endInterval) {
    String selectStatusQuery =
        "select service_name,service_id,service_status as status,service_startts as startts,service_endts as endts,deployment_type from "
        + tableNameServiceAndInfra + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(selectStatusQuery);

    if (previousStartInterval > 0 && endInterval > 0) {
      String idQuery = queryBuilderSelectIdCdTable(accountId, orgId, projectId, previousStartInterval, endInterval);
      idQuery = idQuery.replace(';', ' ');
      totalBuildSqlBuilder.append(String.format(
          "pipeline_execution_summary_cd_id in (%s) and service_name is not null and service_id is not null;",
          idQuery));
    }

    return totalBuildSqlBuilder.toString();
  }

  public TimeAndStatusDeployment queryCalculatorTimeAndStatus(String query) {
    List<String> time = new ArrayList<>();
    List<String> status = new ArrayList<>();

    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(query)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          status.add(resultSet.getString("status"));
          time.add(resultSet.getString("startts"));
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }

    return TimeAndStatusDeployment.builder().status(status).time(time).build();
  }

  public List<String> queryCalculatorEnvType(String queryEnvironmentType) {
    List<String> envType = new ArrayList<>();

    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(queryEnvironmentType)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          envType.add(resultSet.getString("env_type"));
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return envType;
  }

  @Override
  public HealthDeploymentDashboard getHealthDeploymentDashboard(String accountId, String orgId, String projectId,
      long startInterval, long endInterval, long previousStartInterval) {
    endInterval = endInterval + getTimeUnitToGroupBy(DAY);
    String query = queryBuilderSelectStatusTime(accountId, orgId, projectId, previousStartInterval, endInterval);

    List<String> time = new ArrayList<>();
    List<String> status = new ArrayList<>();
    List<String> envType = new ArrayList<>();

    TimeAndStatusDeployment timeAndStatusDeployment = queryCalculatorTimeAndStatus(query);
    time = timeAndStatusDeployment.getTime();
    status = timeAndStatusDeployment.getStatus();

    long total = 0;
    long currentSuccess = 0;
    long currentFailed = 0;
    long previousSuccess = 0;
    long previousFailed = 0;

    HashMap<Long, Integer> totalCountMap = new HashMap<>();
    HashMap<Long, Integer> successCountMap = new HashMap<>();
    HashMap<Long, Integer> failedCountMap = new HashMap<>();

    long startDateCopy = startInterval;
    long endDateCopy = endInterval;

    long timeUnitPerDay = getTimeUnitToGroupBy(DAY);
    while (startDateCopy < endDateCopy) {
      totalCountMap.put(startDateCopy, 0);
      successCountMap.put(startDateCopy, 0);
      failedCountMap.put(startDateCopy, 0);
      startDateCopy = startDateCopy + timeUnitPerDay;
    }

    for (int i = 0; i < time.size(); i++) {
      long currentTimeEpoch = Long.parseLong(time.get(i));
      if (currentTimeEpoch >= startInterval && currentTimeEpoch < endInterval) {
        currentTimeEpoch = getStartingDateEpochValue(currentTimeEpoch, startInterval);
        total++;
        totalCountMap.put(currentTimeEpoch, totalCountMap.get(currentTimeEpoch) + 1);
        if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
          currentSuccess++;
          successCountMap.put(currentTimeEpoch, successCountMap.get(currentTimeEpoch) + 1);
        } else if (failedStatusList.contains(status.get(i))) {
          currentFailed++;
          failedCountMap.put(currentTimeEpoch, failedCountMap.get(currentTimeEpoch) + 1);
        }
      } else {
        if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
          previousSuccess++;
        } else if (failedStatusList.contains(status.get(i))) {
          previousFailed++;
        }
      }
    }

    String queryEnvironmentType = queryBuilderEnvironmentType(accountId, orgId, projectId, startInterval, endInterval);
    envType = queryCalculatorEnvType(queryEnvironmentType);

    long production = Collections.frequency(envType, EnvironmentType.Production.name());
    long nonProduction = Collections.frequency(envType, EnvironmentType.PreProduction.name());

    List<DeploymentDateAndCount> totalDateAndCount = new ArrayList<>();
    List<DeploymentDateAndCount> successDateAndCount = new ArrayList<>();
    List<DeploymentDateAndCount> failedDateAndCount = new ArrayList<>();
    startDateCopy = startInterval;
    endDateCopy = endInterval;

    while (startDateCopy < endDateCopy) {
      totalDateAndCount.add(DeploymentDateAndCount.builder()
                                .time(String.valueOf(startDateCopy))
                                .deployments(Deployment.builder().count(totalCountMap.get(startDateCopy)).build())
                                .build());
      successDateAndCount.add(DeploymentDateAndCount.builder()
                                  .time(String.valueOf(startDateCopy))
                                  .deployments(Deployment.builder().count(successCountMap.get(startDateCopy)).build())
                                  .build());
      failedDateAndCount.add(DeploymentDateAndCount.builder()
                                 .time(String.valueOf(startDateCopy))
                                 .deployments(Deployment.builder().count(failedCountMap.get(startDateCopy)).build())
                                 .build());
      startDateCopy = startDateCopy + timeUnitPerDay;
    }

    return HealthDeploymentDashboard.builder()
        .healthDeploymentInfo(HealthDeploymentInfo.builder()
                                  .total(TotalDeploymentInfo.builder()
                                             .count(total)
                                             .production(production)
                                             .nonProduction(nonProduction)
                                             .countList(totalDateAndCount)
                                             .build())
                                  .success(DeploymentInfo.builder()
                                               .count(currentSuccess)
                                               .rate(getRate(currentSuccess, previousSuccess))
                                               .countList(successDateAndCount)
                                               .build())
                                  .failure(DeploymentInfo.builder()
                                               .count(currentFailed)
                                               .rate(getRate(currentFailed, previousFailed))
                                               .countList(failedDateAndCount)
                                               .build())
                                  .build())
        .build();
  }

  private ExecutionDeployment getExecutionDeployment(String time, long total, long success, long failed) {
    return ExecutionDeployment.builder()
        .time(time)
        .deployments(DeploymentCount.builder().total(total).success(success).failure(failed).build())
        .build();
  }

  public HashMap<String, List<ServiceDeploymentInfo>> queryCalculatorServiceTagMag(String queryServiceTag) {
    HashMap<String, List<ServiceDeploymentInfo>> serviceTagMap = new HashMap<>();

    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(queryServiceTag)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          String planExecutionId = resultSet.getString("pipeline_execution_summary_cd_id");
          String service_name = resultSet.getString("service_name");
          String tag = resultSet.getString("tag");
          if (serviceTagMap.containsKey(planExecutionId)) {
            serviceTagMap.get(planExecutionId).add(getServiceDeployment(service_name, tag));
          } else {
            List<ServiceDeploymentInfo> serviceDeploymentInfos = new ArrayList<>();
            serviceDeploymentInfos.add(getServiceDeployment(service_name, tag));
            serviceTagMap.put(planExecutionId, serviceDeploymentInfos);
          }
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return serviceTagMap;
  }

  @Override
  public ExecutionDeploymentInfo getExecutionDeploymentDashboard(
      String accountId, String orgId, String projectId, long startInterval, long endInterval) {
    endInterval = endInterval + DAY_IN_MS;
    String query = queryBuilderSelectStatusTime(accountId, orgId, projectId, startInterval, endInterval);

    HashMap<Long, Integer> totalCountMap = new HashMap<>();
    HashMap<Long, Integer> successCountMap = new HashMap<>();
    HashMap<Long, Integer> failedCountMap = new HashMap<>();

    long startDateCopy = startInterval;
    long endDateCopy = endInterval;

    long timeUnitPerDay = getTimeUnitToGroupBy(DAY);
    while (startDateCopy < endDateCopy) {
      totalCountMap.put(startDateCopy, 0);
      successCountMap.put(startDateCopy, 0);
      failedCountMap.put(startDateCopy, 0);
      startDateCopy = startDateCopy + timeUnitPerDay;
    }

    TimeAndStatusDeployment timeAndStatusDeployment = queryCalculatorTimeAndStatus(query);
    List<String> time = timeAndStatusDeployment.getTime();
    List<String> status = timeAndStatusDeployment.getStatus();

    List<ExecutionDeployment> executionDeployments = new ArrayList<>();

    for (int i = 0; i < time.size(); i++) {
      long currentTimeEpoch = Long.parseLong(time.get(i));
      currentTimeEpoch = getStartingDateEpochValue(currentTimeEpoch, startInterval);
      totalCountMap.put(currentTimeEpoch, totalCountMap.get(currentTimeEpoch) + 1);
      if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
        successCountMap.put(currentTimeEpoch, successCountMap.get(currentTimeEpoch) + 1);
      } else if (failedStatusList.contains(status.get(i))) {
        failedCountMap.put(currentTimeEpoch, failedCountMap.get(currentTimeEpoch) + 1);
      }
    }

    startDateCopy = startInterval;
    endDateCopy = endInterval;

    while (startDateCopy < endDateCopy) {
      executionDeployments.add(getExecutionDeployment(String.valueOf(startDateCopy), totalCountMap.get(startDateCopy),
          successCountMap.get(startDateCopy), failedCountMap.get(startDateCopy)));
      startDateCopy = startDateCopy + timeUnitPerDay;
    }
    return ExecutionDeploymentInfo.builder().executionDeploymentList(executionDeployments).build();
  }

  @Override
  public ExecutionDeploymentDetailInfo getDeploymentsExecutionInfo(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, long startTime, long endTime) {
    long numberOfDays = (long) Math.ceil((endTime - startTime) / DAY_IN_MS);
    long prevStartTime = startTime - (endTime - startTime + DAY_IN_MS);
    long prevEndTime = startTime - DAY_IN_MS;

    ExecutionDeploymentInfo executionDeploymentInfo =
        getExecutionDeploymentDashboard(accountIdentifier, orgIdentifier, projectIdentifier, startTime, endTime);
    List<ExecutionDeployment> executionDeploymentList = executionDeploymentInfo.getExecutionDeploymentList();

    ExecutionDeploymentInfo prevExecutionDeploymentInfo = getExecutionDeploymentDashboard(
        accountIdentifier, orgIdentifier, projectIdentifier, prevStartTime, prevEndTime);
    List<ExecutionDeployment> prevExecutionDeploymentList = prevExecutionDeploymentInfo.getExecutionDeploymentList();

    long totalDeployments = getTotalDeployments(executionDeploymentList);
    long prevTotalDeployments = getTotalDeployments(prevExecutionDeploymentList);
    double failureRate = getFailureRate(executionDeploymentList);
    double frequency = totalDeployments / numberOfDays;
    double prevFrequency = prevTotalDeployments / numberOfDays;

    double totalDeloymentChangeRate = (totalDeployments - prevTotalDeployments) * 100;
    if (prevTotalDeployments != 0) {
      totalDeloymentChangeRate = totalDeloymentChangeRate / prevTotalDeployments;
    }
    double failureRateChangeRate = getFailureRateChangeRate(executionDeploymentList, prevExecutionDeploymentList);
    double frequencyChangeRate = calculateChangeRate(prevFrequency, frequency);

    return ExecutionDeploymentDetailInfo.builder()
        .startTime(startTime)
        .endTime(endTime)
        .totalDeployments(totalDeployments)
        .failureRate(failureRate)
        .frequency(frequency)
        .totalDeploymentsChangeRate(totalDeloymentChangeRate)
        .failureRateChangeRate(failureRateChangeRate)
        .frequencyChangeRate(frequencyChangeRate)
        .executionDeploymentList(executionDeploymentList)
        .build();
  }

  private double getFailureRateChangeRate(
      List<ExecutionDeployment> executionDeploymentList, List<ExecutionDeployment> prevExecutionDeploymentList) {
    double failureRate = getFailureRate(executionDeploymentList);
    double prevFailureRate = getFailureRate(prevExecutionDeploymentList);
    return calculateChangeRate(prevFailureRate, failureRate);
  }

  private double getFailureRate(List<ExecutionDeployment> executionDeploymentList) {
    long totalDeployments = executionDeploymentList.stream()
                                .map(ExecutionDeployment::getDeployments)
                                .mapToLong(DeploymentCount::getTotal)
                                .sum();
    long totalFailure = executionDeploymentList.stream()
                            .map(ExecutionDeployment::getDeployments)
                            .mapToLong(DeploymentCount::getFailure)
                            .sum();
    double failureRate = totalFailure * 100;
    if (totalDeployments != 0) {
      failureRate = failureRate / totalDeployments;
    }
    return failureRate;
  }
  private double calculateChangeRate(double prevValue, double curValue) {
    double rate = (curValue - prevValue) * 100;
    if (prevValue != 0) {
      rate = rate / prevValue;
    }
    return rate;
  }

  private long getTotalDeployments(List<ExecutionDeployment> executionDeploymentList) {
    long total = 0;
    for (ExecutionDeployment item : executionDeploymentList) {
      total += item.getDeployments().getTotal();
    }
    return total;
  }

  public DeploymentStatusInfoList queryCalculatorDeploymentInfo(String queryStatus) {
    List<String> planExecutionIdList = new ArrayList<>();
    List<String> namePipelineList = new ArrayList<>();
    List<String> startTs = new ArrayList<>();
    List<String> endTs = new ArrayList<>();
    List<String> deploymentStatus = new ArrayList<>();

    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(queryStatus)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          planExecutionIdList.add(resultSet.getString("id"));
          namePipelineList.add(resultSet.getString("name"));
          startTs.add(resultSet.getString("startts"));
          deploymentStatus.add(resultSet.getString("status"));
          if (resultSet.getString("endTs") != null) {
            endTs.add(resultSet.getString("endTs"));
          } else {
            endTs.add(LocalDateTime.now().toString().replace('T', ' '));
          }
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return DeploymentStatusInfoList.builder()
        .planExecutionIdList(planExecutionIdList)
        .deploymentStatus(deploymentStatus)
        .endTs(endTs)
        .namePipelineList(namePipelineList)
        .startTs(startTs)
        .build();
  }

  public List<DeploymentStatusInfo> getDeploymentStatusInfo(String queryStatus, String queryServiceNameTagId) {
    List<String> planExecutionIdList = new ArrayList<>();
    List<String> namePipelineList = new ArrayList<>();
    List<String> startTs = new ArrayList<>();
    List<String> endTs = new ArrayList<>();
    List<String> deploymentStatus = new ArrayList<>();

    HashMap<String, List<ServiceDeploymentInfo>> serviceTagMap = new HashMap<>();

    DeploymentStatusInfoList deploymentStatusInfoList = queryCalculatorDeploymentInfo(queryStatus);
    deploymentStatus = deploymentStatusInfoList.getDeploymentStatus();
    endTs = deploymentStatusInfoList.getEndTs();
    namePipelineList = deploymentStatusInfoList.getNamePipelineList();
    planExecutionIdList = deploymentStatusInfoList.getPlanExecutionIdList();
    startTs = deploymentStatusInfoList.getStartTs();

    String queryServiceTag = queryBuilderServiceTag(queryServiceNameTagId);

    serviceTagMap = queryCalculatorServiceTagMag(queryServiceTag);

    List<DeploymentStatusInfo> statusInfo = new ArrayList<>();
    for (int i = 0; i < planExecutionIdList.size(); i++) {
      String planExecutionId = planExecutionIdList.get(i);
      String startTime = startTs.get(i);
      String endTime = endTs.get(i);
      statusInfo.add(this.getDeploymentStatusInfoObject(
          namePipelineList.get(i), startTime, endTime, deploymentStatus.get(i), serviceTagMap.get(planExecutionId)));
    }
    return statusInfo;
  }
  @Override
  public DashboardDeploymentActiveFailedRunningInfo getDeploymentActiveFailedRunningInfo(
      String accountId, String orgId, String projectId, long days) {
    // failed
    String queryFailed = queryBuilderStatus(accountId, orgId, projectId, days, failedStatusList);
    String queryServiceNameTagIdFailed =
        queryBuilderSelectIdLimitTimeCdTable(accountId, orgId, projectId, days, failedStatusList);
    List<DeploymentStatusInfo> failure = getDeploymentStatusInfo(queryFailed, queryServiceNameTagIdFailed);

    // active
    String queryActive = queryBuilderStatus(accountId, orgId, projectId, days, activeStatusList);
    String queryServiceNameTagIdActive =
        queryBuilderSelectIdLimitTimeCdTable(accountId, orgId, projectId, days, activeStatusList);
    List<DeploymentStatusInfo> active = getDeploymentStatusInfo(queryActive, queryServiceNameTagIdActive);

    // pending
    String queryPending = queryBuilderStatus(accountId, orgId, projectId, days, pendingStatusList);
    String queryServiceNameTagIdPending =
        queryBuilderSelectIdLimitTimeCdTable(accountId, orgId, projectId, days, pendingStatusList);
    List<DeploymentStatusInfo> pending = getDeploymentStatusInfo(queryPending, queryServiceNameTagIdPending);

    return DashboardDeploymentActiveFailedRunningInfo.builder()
        .failure(failure)
        .active(active)
        .pending(pending)
        .build();
  }

  private DeploymentStatusInfo getDeploymentStatusInfoObject(String name, String startTime, String endTime,
      String status, List<ServiceDeploymentInfo> serviceDeploymentInfos) {
    return DeploymentStatusInfo.builder()
        .name(name)
        .startTs(startTime)
        .endTs(endTime)
        .status(status)
        .serviceInfoList(serviceDeploymentInfos)
        .build();
  }

  private ServiceDeploymentInfo getServiceDeployment(String service_name, String tag) {
    if (service_name != null) {
      if (tag != null) {
        return ServiceDeploymentInfo.builder().serviceName(service_name).serviceTag(tag).build();
      } else {
        return ServiceDeploymentInfo.builder().serviceName(service_name).build();
      }
    }
    return ServiceDeploymentInfo.builder().build();
  }

  private WorkloadDeploymentInfo getWorkloadDeploymentInfo(String workload, String workloadId, String lastExecuted,
      long totalDeployment, String lastStatus, String deploymentType, long success, long previousSuccess,
      List<WorkloadDateCountInfo> dateCount) {
    double percentSuccess = 0.0;
    if (totalDeployment != 0) {
      percentSuccess = success / (double) totalDeployment;
      percentSuccess = percentSuccess * 100.0;
    }
    return WorkloadDeploymentInfo.builder()
        .serviceName(workload)
        .serviceId(workloadId)
        .lastExecuted(lastExecuted)
        .totalDeployments(totalDeployment)
        .lastStatus(lastStatus)
        .percentSuccess(percentSuccess)
        .deploymentType(deploymentType)
        .rateSuccess(getRate(success, previousSuccess))
        .workload(dateCount)
        .build();
  }

  public DashboardWorkloadDeployment getWorkloadDeploymentInfoCalculation(List<String> workloadsId, List<String> status,
      List<String> startTs, List<String> deploymentTypeList, HashMap<String, String> uniqueWorkloadNameAndId,
      long startDate, long endDate) {
    List<WorkloadDeploymentInfo> workloadDeploymentInfoList = new ArrayList<>();

    for (String workloadId : uniqueWorkloadNameAndId.keySet()) {
      long totalDeployment = 0;
      long success = 0;
      long previousSuccess = 0;
      long lastExecuted = 0L;
      String lastStatus = null;
      String deploymentType = null;

      HashMap<Long, Integer> deploymentCountMap = new HashMap<>();

      long startDateCopy = startDate;
      long endDateCopy = endDate;

      while (startDateCopy < endDateCopy) {
        deploymentCountMap.put(startDateCopy, 0);
        startDateCopy = startDateCopy + DAY_IN_MS;
      }

      for (int i = 0; i < workloadsId.size(); i++) {
        if (workloadsId.get(i).contentEquals(workloadId)) {
          long currentTimeEpoch = Long.parseLong(startTs.get(i));
          if (currentTimeEpoch >= startDate && currentTimeEpoch < endDate) {
            currentTimeEpoch = getStartingDateEpochValue(currentTimeEpoch, startDate);
            totalDeployment++;
            deploymentCountMap.put(currentTimeEpoch, deploymentCountMap.get(currentTimeEpoch) + 1);
            if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
              success++;
            }
            if (lastExecuted == 0) {
              lastExecuted = Long.parseLong(startTs.get(i));
              lastStatus = status.get(i);
              deploymentType = deploymentTypeList.get(i);
            } else {
              if (lastExecuted < Long.parseLong(startTs.get(i))) {
                lastExecuted = Long.parseLong(startTs.get(i));
                lastStatus = status.get(i);
                deploymentType = deploymentTypeList.get(i);
              }
            }
          } else {
            if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
              previousSuccess++;
            }
          }
        }
      }

      if (totalDeployment > 0) {
        List<WorkloadDateCountInfo> dateCount = new ArrayList<>();
        startDateCopy = startDate;
        endDateCopy = endDate;
        while (startDateCopy < endDateCopy) {
          dateCount.add(WorkloadDateCountInfo.builder()
                            .date(String.valueOf(startDateCopy))
                            .execution(WorkloadCountInfo.builder().count(deploymentCountMap.get(startDateCopy)).build())
                            .build());
          startDateCopy = startDateCopy + DAY_IN_MS;
        }
        workloadDeploymentInfoList.add(
            getWorkloadDeploymentInfo(uniqueWorkloadNameAndId.get(workloadId), workloadId, String.valueOf(lastExecuted),
                totalDeployment, lastStatus, deploymentType, success, previousSuccess, dateCount));
      }
    }
    return DashboardWorkloadDeployment.builder().workloadDeploymentInfoList(workloadDeploymentInfoList).build();
  }

  @Override
  public DashboardWorkloadDeployment getDashboardWorkloadDeployment(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, long startInterval, long endInterval, long previousStartInterval) {
    endInterval = endInterval + DAY_IN_MS;
    String query = queryBuilderSelectWorkload(
        accountIdentifier, orgIdentifier, projectIdentifier, previousStartInterval, endInterval);

    List<String> workloadsId = new ArrayList<>();
    List<String> status = new ArrayList<>();
    List<String> startTs = new ArrayList<>();
    List<String> endTs = new ArrayList<>();
    List<String> deploymentTypeList = new ArrayList<>();

    HashMap<String, String> uniqueWorkloadNameAndId = new HashMap<>();

    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(query)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          String serviceName = resultSet.getString("service_name");
          String service_id = resultSet.getString("service_id");
          String startTime = resultSet.getString("startTs");
          workloadsId.add(service_id);
          status.add(resultSet.getString("status"));
          startTs.add(startTime);
          endTs.add(resultSet.getString("endTs"));
          deploymentTypeList.add(resultSet.getString("deployment_type"));

          if (!uniqueWorkloadNameAndId.containsKey(service_id)) {
            uniqueWorkloadNameAndId.put(service_id, serviceName);
          }
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return getWorkloadDeploymentInfoCalculation(
        workloadsId, status, startTs, deploymentTypeList, uniqueWorkloadNameAndId, startInterval, endInterval);
  }

  public long getTimeUnitToGroupBy(TimeGroupType timeGroupType) {
    if (timeGroupType == DAY) {
      return DAY_IN_MS;
    } else if (timeGroupType == HOUR) {
      return HOUR_IN_MS;
    } else {
      throw new UnknownEnumTypeException("Time Group Type", String.valueOf(timeGroupType));
    }
  }

  public long getStartingDateEpochValue(long epochValue, long startInterval) {
    return epochValue - epochValue % DAY_IN_MS;
  }
}
