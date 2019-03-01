package io.harness.RestUtils;

import com.google.inject.Singleton;

import io.harness.framework.Setup;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;

import javax.ws.rs.core.GenericType;

@Singleton
public class WorkflowRestUtil extends AbstractFunctionalTest {
  public Workflow createWorkflow(String accountId, String appId, Workflow workflow) throws Exception {
    GenericType<RestResponse<Workflow>> workflowType = new GenericType<RestResponse<Workflow>>() {};

    RestResponse<Workflow> savedWorkflowResponse = Setup.portal()
                                                       .auth()
                                                       .oauth2(bearerToken)
                                                       .queryParam("accountId", accountId)
                                                       .queryParam("appId", appId)
                                                       .body(workflow, ObjectMapperType.GSON)
                                                       .contentType(ContentType.JSON)
                                                       .post("/workflows")
                                                       .as(workflowType.getType());

    if (savedWorkflowResponse.getResource() == null) {
      throw new Exception(String.valueOf(savedWorkflowResponse.getResponseMessages()));
    }

    return savedWorkflowResponse.getResource();
  }

  public WorkflowExecution runWorkflow(String appId, String envId, ExecutionArgs executionArgs) {
    GenericType<RestResponse<WorkflowExecution>> workflowExecutionType =
        new GenericType<RestResponse<WorkflowExecution>>() {};

    RestResponse<WorkflowExecution> savedWorkflowExecutionResponse = Setup.portal()
                                                                         .auth()
                                                                         .oauth2(bearerToken)
                                                                         .queryParam("appId", appId)
                                                                         .queryParam("envId", envId)
                                                                         .contentType(ContentType.JSON)
                                                                         .body(executionArgs, ObjectMapperType.GSON)
                                                                         .post("/executions")
                                                                         .as(workflowExecutionType.getType());

    return savedWorkflowExecutionResponse.getResource();
  }

  public WorkflowPhase saveWorkflowPhase(String appId, String workflowId, String phaseId, WorkflowPhase phase) {
    GenericType<RestResponse<WorkflowPhase>> workflowExecutionType = new GenericType<RestResponse<WorkflowPhase>>() {};

    RestResponse<WorkflowPhase> savedWorkflowPhaseResponse = Setup.portal()
                                                                 .auth()
                                                                 .oauth2(bearerToken)
                                                                 .queryParam("appId", appId)
                                                                 .contentType(ContentType.JSON)
                                                                 .body(phase, ObjectMapperType.GSON)
                                                                 .put("/workflows/" + workflowId + "/phases/" + phaseId)
                                                                 .as(workflowExecutionType.getType());

    return savedWorkflowPhaseResponse.getResource();
  }

  public Object deleteWorkflow(String workflowId, String appId) {
    GenericType<RestResponse> workflowType = new GenericType<RestResponse>() {};
    RestResponse savedResponse = Setup.portal()
                                     .auth()
                                     .oauth2(bearerToken)
                                     .contentType(ContentType.JSON)
                                     .queryParam("appId", appId)
                                     .queryParam("sort[0][field]", "createdAt")
                                     .queryParam("sort[0][direction]", "DESC")
                                     .delete("/workflows/" + workflowId)
                                     .as(workflowType.getType());

    return savedResponse.getResource();
  }
}
