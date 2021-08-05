package io.harness.cvng.analysis.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.NEMANJA;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.TimeSeriesAnalysisSummary;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.beans.TransactionMetricInfo;
import io.harness.cvng.analysis.beans.TransactionMetricInfoSummaryPageDTO;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.job.CanaryVerificationJobDTO;
import io.harness.cvng.beans.job.Sensitivity;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class DeploymentTimeSeriesAnalysisServiceImplTest extends CvNextGenTestBase {
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private VerificationJobService verificationJobService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private DeploymentTimeSeriesAnalysisService deploymentTimeSeriesAnalysisService;
  @Inject private CVConfigService cvConfigService;
  @Mock private NextGenService nextGenService;

  private String accountId;
  private String identifier;
  private String serviceIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;
  private String envIdentifier;
  private BuilderFactory builderFactory;

  @Before
  public void setUp() throws IllegalAccessException {
    accountId = generateUuid();
    serviceIdentifier = generateUuid();
    identifier = generateUuid();
    projectIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    envIdentifier = generateUuid();
    builderFactory = BuilderFactory.builder()
                         .context(BuilderFactory.Context.builder()
                                      .accountId(accountId)
                                      .envIdentifier(envIdentifier)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .serviceIdentifier(serviceIdentifier)
                                      .build())
                         .build();

    FieldUtils.writeField(deploymentTimeSeriesAnalysisService, "nextGenService", nextGenService, true);
    when(nextGenService.get(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Optional.of(ConnectorInfoDTO.builder().name("AppDynamics Connector").build()));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetAnalysisSummary() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());

    deploymentTimeSeriesAnalysisService.save(createDeploymentTimeSeriesAnalysis(verificationTaskId));

    TimeSeriesAnalysisSummary summary =
        deploymentTimeSeriesAnalysisService.getAnalysisSummary(Arrays.asList(verificationJobInstanceId));
    assertThat(summary).isNotNull();
    assertThat(summary.getNumAnomMetrics()).isEqualTo(1);
    assertThat(summary.getTotalNumMetrics()).isEqualTo(2);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetAnalysisSummary_badRequest() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());

    deploymentTimeSeriesAnalysisService.save(createDeploymentTimeSeriesAnalysis(verificationTaskId));

    TimeSeriesAnalysisSummary summary =
        deploymentTimeSeriesAnalysisService.getAnalysisSummary(Arrays.asList(verificationJobInstanceId));

    assertThatThrownBy(() -> deploymentTimeSeriesAnalysisService.getAnalysisSummary(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Missing verificationJobInstanceIds when looking for summary");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetAnalysisSummary_noAnalysisYet() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());

    TimeSeriesAnalysisSummary summary =
        deploymentTimeSeriesAnalysisService.getAnalysisSummary(Arrays.asList(verificationJobInstanceId));

    assertThat(summary).isNotNull();
    assertThat(summary.getNumAnomMetrics()).isEqualTo(0);
    assertThat(summary.getTotalNumMetrics()).isEqualTo(0);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());

    deploymentTimeSeriesAnalysisService.save(createDeploymentTimeSeriesAnalysis(verificationTaskId));

    TransactionMetricInfoSummaryPageDTO transactionMetricInfoSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, null, null, 0);

    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getTotalPages()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent()).isNotNull();
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse()
                   .getContent()
                   .get(0)
                   .getTransactionMetric()
                   .getTransactionName())
        .isEqualTo("/todolist/exception");
    assertThat(
        transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getTransactionMetric().getScore())
        .isEqualTo(2.5); // ensures that sorting based on score from transaction works
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getConnectorName())
        .isEqualTo("AppDynamics Connector");
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getNodes().size())
        .isEqualTo(3);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse()
                   .getContent()
                   .get(0)
                   .getNodes()
                   .first()
                   .getHostName()
                   .get())
        .isEqualTo("node3");
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getNodes().first().getScore())
        .isEqualTo(2.0); // checks that sorting per node works correctly
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetMetrics_withNoVerificationTaskMapping() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    String verificationJobInstanceId = verificationJobInstanceService.create(createVerificationJobInstance());
    TransactionMetricInfoSummaryPageDTO transactionMetricInfoSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, null, null, 0);

    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getTotalPages()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent()).isNotNull();
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent()).isEmpty();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withHostNameFilter() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());
    deploymentTimeSeriesAnalysisService.save(createDeploymentTimeSeriesAnalysis(verificationTaskId));

    TransactionMetricInfoSummaryPageDTO transactionMetricInfoSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, "node1", null, 0);

    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getTotalPages()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent()).isNotNull();
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getConnectorName())
        .isEqualTo("AppDynamics Connector");
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getNodes().size())
        .isEqualTo(1);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse()
                   .getContent()
                   .get(0)
                   .getNodes()
                   .first()
                   .getHostName()
                   .get())
        .isEqualTo("node1");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetMetrics_withHostNameFilterWithOnlyFewTxn() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());
    deploymentTimeSeriesAnalysisService.save(createDeploymentTimeSeriesAnalysis(verificationTaskId));

    TransactionMetricInfoSummaryPageDTO transactionMetricInfoSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, "node3", null, 0);

    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getTotalPages()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent()).isNotNull();
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getConnectorName())
        .isEqualTo("AppDynamics Connector");
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().size()).isEqualTo(1);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse()
                   .getContent()
                   .get(0)
                   .getNodes()
                   .first()
                   .getHostName()
                   .get())
        .isEqualTo("node3");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withWrongHostName() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());
    deploymentTimeSeriesAnalysisService.save(createDeploymentTimeSeriesAnalysis(verificationTaskId));
    TransactionMetricInfoSummaryPageDTO summaryPageDTO = deploymentTimeSeriesAnalysisService.getMetrics(
        accountId, verificationJobInstanceId, false, "randomNode", null, 0);
    assertThat(summaryPageDTO).isNotNull();
    assertThat(summaryPageDTO.getPageResponse().getTotalItems()).isEqualTo(0);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withAnomalousMetricsFilter() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());
    deploymentTimeSeriesAnalysisService.save(createDeploymentTimeSeriesAnalysis(verificationTaskId));

    TransactionMetricInfoSummaryPageDTO transactionMetricInfoSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, true, null, null, 0);

    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getTotalPages()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent()).isNotNull();
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().size()).isEqualTo(1);
    assertThat(
        transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getTransactionMetric().getScore())
        .isEqualTo(2.5);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getConnectorName())
        .isEqualTo("AppDynamics Connector");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withHostNameAndAnomalousMetricsFilter() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());
    deploymentTimeSeriesAnalysisService.save(createDeploymentTimeSeriesAnalysis(verificationTaskId));

    TransactionMetricInfoSummaryPageDTO transactionMetricInfoSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, true, "node2", null, 0);

    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getTotalPages()).isEqualTo(0);
    List<TransactionMetricInfo> content = transactionMetricInfoSummaryPageDTO.getPageResponse().getContent();
    assertThat(content).isNotNull();
    assertThat(content.size()).isEqualTo(2);
    assertThat(content.get(0).getTransactionMetric().getScore()).isEqualTo(2.5);
    assertThat(content.get(0).getConnectorName()).isEqualTo("AppDynamics Connector");
    assertThat(content.get(0).getNodes().size()).isEqualTo(1);
    assertThat(content.get(0).getNodes().first().getHostName().get()).isEqualTo("node2");

    assertThat(content.get(0).getTransactionMetric().getTransactionName()).isEqualTo("/todolist/exception");
    assertThat(content.get(0).getTransactionMetric().getMetricName()).isEqualTo("Calls per Minute");
    assertThat(content.get(1).getTransactionMetric().getTransactionName()).isEqualTo("/todolist/inside");
    assertThat(content.get(1).getTransactionMetric().getMetricName()).isEqualTo("Errors per Minute");
  }
  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withMultipleDeploymentTimeSeriesAnalyses() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());
    deploymentTimeSeriesAnalysisService.save(createDeploymentTimeSeriesAnalysis(verificationTaskId));

    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimeSeriesAnalysis(verificationTaskId);
    deploymentTimeSeriesAnalysis.setStartTime(Instant.now().plus(1, ChronoUnit.HOURS));
    DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData transactionMetricHostData =
        createTransactionMetricHostData("newTransaction", "newMetric", 2, 5.0,
            deploymentTimeSeriesAnalysis.getTransactionMetricSummaries().get(0).getHostData());
    deploymentTimeSeriesAnalysis.setTransactionMetricSummaries(Arrays.asList(transactionMetricHostData));
    deploymentTimeSeriesAnalysisService.save(deploymentTimeSeriesAnalysis);

    TransactionMetricInfoSummaryPageDTO transactionMetricInfoSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, null, null, 0);

    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getTotalPages()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent()).isNotNull();
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().size()).isEqualTo(1);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse()
                   .getContent()
                   .get(0)
                   .getTransactionMetric()
                   .getTransactionName())
        .isEqualTo("newTransaction");
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getConnectorName())
        .isEqualTo("AppDynamics Connector");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withMultiplePages() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimeSeriesAnalysis(verificationTaskId);
    List<DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData> transactionSummaries = new ArrayList();
    for (int i = 0; i < 25; i++) {
      transactionSummaries.add(createTransactionMetricHostData("transaction " + i, "metric", 0, 0.0,
          deploymentTimeSeriesAnalysis.getTransactionMetricSummaries().get(0).getHostData()));
    }
    deploymentTimeSeriesAnalysis.setTransactionMetricSummaries(transactionSummaries);
    deploymentTimeSeriesAnalysisService.save(deploymentTimeSeriesAnalysis);

    TransactionMetricInfoSummaryPageDTO page1 =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, null, null, 0);

    assertThat(page1.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(page1.getPageResponse().getTotalPages()).isEqualTo(2);
    assertThat(page1.getPageResponse().getContent()).isNotNull();
    assertThat(page1.getPageResponse().getContent().size()).isEqualTo(10);

    TransactionMetricInfoSummaryPageDTO page2 =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, null, null, 1);

    assertThat(page2.getPageResponse().getPageIndex()).isEqualTo(1);
    assertThat(page2.getPageResponse().getTotalPages()).isEqualTo(2);
    assertThat(page2.getPageResponse().getContent()).isNotNull();
    assertThat(page2.getPageResponse().getContent().size()).isEqualTo(10);

    TransactionMetricInfoSummaryPageDTO page3 =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, null, null, 2);

    assertThat(page3.getPageResponse().getPageIndex()).isEqualTo(2);
    assertThat(page3.getPageResponse().getTotalPages()).isEqualTo(2);
    assertThat(page3.getPageResponse().getContent()).isNotNull();
    assertThat(page3.getPageResponse().getContent().size()).isEqualTo(5);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withoutDeploymentTimeSeriesAnalysis() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());

    TransactionMetricInfoSummaryPageDTO transactionMetricInfoSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, null, null, 0);

    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getTotalPages()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent()).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRecentHighestRiskScore_noData() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());
    assertThat(deploymentTimeSeriesAnalysisService.getRecentHighestRiskScore(accountId, verificationJobInstanceId))
        .isEqualTo(Optional.empty());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRecentHighestRiskScore_verificationTaskIdDoesNotExists() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    String verificationJobInstanceId = verificationJobInstanceService.create(createVerificationJobInstance());
    assertThatThrownBy(
        () -> deploymentTimeSeriesAnalysisService.getRecentHighestRiskScore(accountId, verificationJobInstanceId))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRecentHighestRiskScore_getLatest() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimeSeriesAnalysis(verificationTaskId);
    List<DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData> transactionSummaries = new ArrayList();
    for (int i = 0; i < 25; i++) {
      transactionSummaries.add(createTransactionMetricHostData("transaction " + i, "metric", 0, 0.0,
          deploymentTimeSeriesAnalysis.getTransactionMetricSummaries().get(0).getHostData()));
    }
    deploymentTimeSeriesAnalysis.setTransactionMetricSummaries(transactionSummaries);
    deploymentTimeSeriesAnalysisService.save(deploymentTimeSeriesAnalysis);
    assertThat(
        deploymentTimeSeriesAnalysisService.getRecentHighestRiskScore(accountId, verificationJobInstanceId).get())
        .isEqualTo(Risk.MEDIUM);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRecentHighestRiskScore_getRecentHighest() {
    verificationJobService.create(accountId, createCanaryVerificationJobDTO());
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId1 =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());
    cvConfig = createCVConfig();
    String verificationTaskId2 =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());

    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis1 =
        createDeploymentTimeSeriesAnalysis(verificationTaskId1);
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis2 =
        createDeploymentTimeSeriesAnalysis(verificationTaskId2);
    deploymentTimeSeriesAnalysis2.setStartTime(
        deploymentTimeSeriesAnalysis1.getStartTime().minus(Duration.ofMinutes(2)));
    deploymentTimeSeriesAnalysis2.setEndTime(deploymentTimeSeriesAnalysis1.getStartTime().minus(Duration.ofMinutes(1)));
    deploymentTimeSeriesAnalysis2.setScore(.9);
    List<DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData> transactionSummaries = new ArrayList();
    for (int i = 0; i < 25; i++) {
      transactionSummaries.add(createTransactionMetricHostData("transaction " + i, "metric", 0, 0.0,
          deploymentTimeSeriesAnalysis1.getTransactionMetricSummaries().get(0).getHostData()));
    }
    deploymentTimeSeriesAnalysis1.setTransactionMetricSummaries(transactionSummaries);
    deploymentTimeSeriesAnalysis2.setTransactionMetricSummaries(transactionSummaries);
    deploymentTimeSeriesAnalysisService.save(deploymentTimeSeriesAnalysis1);
    deploymentTimeSeriesAnalysisService.save(deploymentTimeSeriesAnalysis2);
    assertThat(
        deploymentTimeSeriesAnalysisService.getRecentHighestRiskScore(accountId, verificationJobInstanceId).get())
        .isEqualTo(Risk.MEDIUM);
  }

  private VerificationJobInstance createVerificationJobInstance() {
    VerificationJobInstance jobInstance = builderFactory.verificationJobInstanceBuilder().build();
    jobInstance.setAccountId(accountId);
    return jobInstance;
  }

  private CanaryVerificationJobDTO createCanaryVerificationJobDTO() {
    CanaryVerificationJobDTO canaryVerificationJobDTO = new CanaryVerificationJobDTO();
    canaryVerificationJobDTO.setIdentifier(identifier);
    canaryVerificationJobDTO.setJobName("jobName");
    canaryVerificationJobDTO.setDuration("100");
    canaryVerificationJobDTO.setServiceIdentifier(serviceIdentifier);
    canaryVerificationJobDTO.setProjectIdentifier(projectIdentifier);
    canaryVerificationJobDTO.setOrgIdentifier(orgIdentifier);
    canaryVerificationJobDTO.setEnvIdentifier(envIdentifier);
    canaryVerificationJobDTO.setDataSources(Arrays.asList(DataSourceType.APP_DYNAMICS));
    canaryVerificationJobDTO.setMonitoringSources(Arrays.asList(generateUuid()));
    canaryVerificationJobDTO.setSensitivity(Sensitivity.LOW.name());
    return canaryVerificationJobDTO;
  }

  private DeploymentTimeSeriesAnalysisDTO.HostInfo createHostInfo(
      String hostName, int risk, Double score, boolean primary, boolean canary) {
    return DeploymentTimeSeriesAnalysisDTO.HostInfo.builder()
        .hostName(hostName)
        .risk(risk)
        .score(score)
        .primary(primary)
        .canary(canary)
        .build();
  }

  private DeploymentTimeSeriesAnalysisDTO.HostData createHostData(
      String hostName, int risk, Double score, List<Double> controlData, List<Double> testData) {
    return DeploymentTimeSeriesAnalysisDTO.HostData.builder()
        .hostName(hostName)
        .risk(risk)
        .score(score)
        .controlData(controlData)
        .testData(testData)
        .build();
  }

  private DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData createTransactionMetricHostData(
      String transactionName, String metricName, int risk, Double score,
      List<DeploymentTimeSeriesAnalysisDTO.HostData> hostDataList) {
    return DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData.builder()
        .transactionName(transactionName)
        .metricName(metricName)
        .risk(risk)
        .score(score)
        .hostData(hostDataList)
        .build();
  }

  private DeploymentTimeSeriesAnalysis createDeploymentTimeSeriesAnalysis(String verificationTaskId) {
    DeploymentTimeSeriesAnalysisDTO.HostInfo hostInfo1 = createHostInfo("node1", 1, 1.1, false, true);
    DeploymentTimeSeriesAnalysisDTO.HostInfo hostInfo2 = createHostInfo("node2", 2, 2.2, false, true);
    DeploymentTimeSeriesAnalysisDTO.HostInfo hostInfo3 = createHostInfo("node3", 2, 2.2, false, true);
    DeploymentTimeSeriesAnalysisDTO.HostData hostData1 =
        createHostData("node1", 0, 0.0, Arrays.asList(1D), Arrays.asList(1D));
    DeploymentTimeSeriesAnalysisDTO.HostData hostData2 =
        createHostData("node2", 2, 2.0, Arrays.asList(1D), Arrays.asList(1D));

    DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData transactionMetricHostData1 =
        createTransactionMetricHostData(
            "/todolist/inside", "Errors per Minute", 0, 0.5, Arrays.asList(hostData1, hostData2));

    DeploymentTimeSeriesAnalysisDTO.HostData hostData3 =
        createHostData("node1", 0, 0.0, Arrays.asList(1D), Arrays.asList(1D));
    DeploymentTimeSeriesAnalysisDTO.HostData hostData4 =
        createHostData("node2", 2, 2.0, Arrays.asList(1D), Arrays.asList(1D));
    DeploymentTimeSeriesAnalysisDTO.HostData hostData5 =
        createHostData("node3", 2, 2.0, Arrays.asList(1D), Arrays.asList(1D));

    DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData transactionMetricHostData2 =
        createTransactionMetricHostData(
            "/todolist/exception", "Calls per Minute", 2, 2.5, Arrays.asList(hostData3, hostData4, hostData5));
    return DeploymentTimeSeriesAnalysis.builder()
        .accountId(accountId)
        .score(.7)
        .risk(Risk.MEDIUM)
        .verificationTaskId(verificationTaskId)
        .transactionMetricSummaries(Arrays.asList(transactionMetricHostData1, transactionMetricHostData2))
        .hostSummaries(Arrays.asList(hostInfo1, hostInfo2, hostInfo3))
        .startTime(Instant.now())
        .endTime(Instant.now().plus(1, ChronoUnit.MINUTES))
        .build();
  }

  private CVConfig createCVConfig() {
    CVConfig cvConfig = builderFactory.appDynamicsCVConfigBuilder().build();
    return cvConfigService.save(cvConfig);
  }
}
