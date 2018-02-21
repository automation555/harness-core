package software.wings.service.impl.analysis;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.common.UUIDGenerator.generateUuid;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask.LearningEngineAnalysisTaskBuilder;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;

import java.util.Set;

/**
 * Created by sriram_parthasarathy on 8/23/17.
 */
public class LogMLAnalysisGenerator implements Runnable {
  public static final int PYTHON_JOB_RETRIES = 3;

  @SchemaIgnore @Transient private static final Logger logger = LoggerFactory.getLogger(LogMLAnalysisGenerator.class);

  public static final String LOG_ML_ROOT = "SPLUNKML_ROOT";
  protected static final String LOG_ML_SHELL_FILE_NAME = "run_splunkml.sh";

  private final AnalysisContext context;
  private final String pythonScriptRoot;
  private final String accountId;
  private final String applicationId;
  private final String workflowId;
  private final String serviceId;
  private final Set<String> testNodes;
  private final Set<String> controlNodes;
  private final Set<String> queries;
  private int logAnalysisMinute;
  private AnalysisService analysisService;
  private LearningEngineService learningEngineService;

  public LogMLAnalysisGenerator(AnalysisContext context, int logAnalysisMinute, AnalysisService analysisService,
      LearningEngineService learningEngineService) {
    this.context = context;
    this.analysisService = analysisService;
    this.pythonScriptRoot = System.getenv(LOG_ML_ROOT);
    Preconditions.checkState(isNotBlank(pythonScriptRoot), "SPLUNKML_ROOT can not be null or empty");

    this.applicationId = context.getAppId();
    this.accountId = context.getAccountId();
    this.workflowId = context.getWorkflowId();
    this.serviceId = context.getServiceId();
    this.testNodes = context.getTestNodes();
    this.controlNodes = context.getControlNodes();
    //    if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
    //      this.controlNodes.removeAll(this.testNodes);
    //    }
    this.queries = context.getQueries();
    this.logAnalysisMinute = logAnalysisMinute;
    this.learningEngineService = learningEngineService;
  }

  @Override
  public void run() {
    generateAnalysis();
    logAnalysisMinute++;
  }

  private void generateAnalysis() {
    try {
      for (String query : queries) {
        String uuid = generateUuid();
        // TODO fix this
        if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
            && !analysisService.isLogDataCollected(
                   applicationId, context.getStateExecutionId(), query, logAnalysisMinute, context.getStateType())) {
          logger.warn("No data collected for minute " + logAnalysisMinute + " for application: " + applicationId
              + " stateExecution: " + context.getStateExecutionId() + ". No ML analysis will be run this minute");
          continue;
        }

        final String lastWorkflowExecutionId = analysisService.getLastSuccessfulWorkflowExecutionIdWithLogs(
            context.getStateType(), applicationId, serviceId, query, workflowId);
        final boolean isBaselineCreated =
            context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
            || !lastWorkflowExecutionId.equals("-1");

        String testInputUrl = "/api/" + context.getStateBaseUrl() + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL
            + "?accountId=" + accountId + "&clusterLevel=" + ClusterLevel.L2.name()
            + "&workflowExecutionId=" + context.getWorkflowExecutionId() + "&compareCurrent=true";

        String controlInputUrl;

        if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
          controlInputUrl = "/api/" + context.getStateBaseUrl() + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL
              + "?accountId=" + accountId + "&clusterLevel=" + ClusterLevel.L2.name()
              + "&workflowExecutionId=" + context.getWorkflowExecutionId() + "&compareCurrent=true";
        } else {
          controlInputUrl = "/api/" + context.getStateBaseUrl() + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL
              + "?accountId=" + accountId + "&clusterLevel=" + ClusterLevel.L2.name()
              + "&workflowExecutionId=" + lastWorkflowExecutionId + "&compareCurrent=false";
        }

        final String logAnalysisSaveUrl = "/api/" + context.getStateBaseUrl()
            + LogAnalysisResource.ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL + "?accountId=" + accountId
            + "&applicationId=" + applicationId + "&stateExecutionId=" + context.getStateExecutionId()
            + "&logCollectionMinute=" + logAnalysisMinute + "&isBaselineCreated=" + isBaselineCreated
            + "&taskId=" + uuid;
        final String logAnalysisGetUrl = "/api/" + context.getStateBaseUrl()
            + LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_RECORDS_URL + "?accountId=" + accountId;

        LearningEngineAnalysisTaskBuilder analysisTaskBuilder =
            LearningEngineAnalysisTask.builder()
                .ml_shell_file_name(LOG_ML_SHELL_FILE_NAME)
                .query(Lists.newArrayList(query.split(" ")))
                .workflow_id(context.getWorkflowId())
                .workflow_execution_id(context.getWorkflowExecutionId())
                .state_execution_id(context.getStateExecutionId())
                .service_id(context.getServiceId())
                .sim_threshold(0.9)
                .analysis_minute(logAnalysisMinute)
                .analysis_save_url(logAnalysisSaveUrl)
                .log_analysis_get_url(logAnalysisGetUrl)
                .ml_analysis_type(MLAnalysisType.LOG_ML)
                .stateType(context.getStateType());

        if (isBaselineCreated) {
          analysisTaskBuilder.control_input_url(controlInputUrl)
              .test_input_url(testInputUrl)
              .control_nodes(controlNodes)
              .test_nodes(testNodes);
        } else {
          analysisTaskBuilder.control_input_url(testInputUrl).control_nodes(testNodes);
        }

        LearningEngineAnalysisTask analysisTask = analysisTaskBuilder.build();
        analysisTask.setAppId(applicationId);
        analysisTask.setUuid(uuid);
        learningEngineService.addLearningEngineAnalysisTask(analysisTask);
      }
    } catch (Exception e) {
      throw new RuntimeException(
          "Log analysis failed for " + context.getStateExecutionId() + " for minute " + logAnalysisMinute, e);
    }
  }
}
