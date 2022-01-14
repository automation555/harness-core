package software.wings.search.entities.workflow;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import software.wings.audit.AuditHeader;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.ElasticsearchRequestHandler;
import software.wings.search.framework.SearchEntity;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class WorkflowSearchEntity implements SearchEntity<Workflow> {
  @Inject private WorkflowViewBuilder workflowViewBuilder;
  @Inject private WorkflowChangeHandler workflowChangeHandler;
  @Inject private WorkflowElasticsearchRequestHandler workflowSearchRequestHandler;

  public static final String TYPE = "workflows";
  public static final String VERSION = "0.2";
  public static final Class<Workflow> SOURCE_ENTITY_CLASS = Workflow.class;
  private static final String CONFIGURATION_PATH = "workflow/WorkflowSchema.json";
  private static final List<Class<? extends PersistentEntity>> SUBSCRIPTION_ENTITIES =
      ImmutableList.<Class<? extends PersistentEntity>>builder()
          .add(Application.class)
          .add(Service.class)
          .add(Workflow.class)
          .add(Environment.class)
          .add(WorkflowExecution.class)
          .add(AuditHeader.class)
          .add(Pipeline.class)
          .build();

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public Class<Workflow> getSourceEntityClass() {
    return SOURCE_ENTITY_CLASS;
  }

  @Override
  public List<Class<? extends PersistentEntity>> getSubscriptionEntities() {
    return SUBSCRIPTION_ENTITIES;
  }

  @Override
  public String getConfigurationPath() {
    return CONFIGURATION_PATH;
  }

  @Override
  public ChangeHandler getChangeHandler() {
    return workflowChangeHandler;
  }

  @Override
  public ElasticsearchRequestHandler getElasticsearchRequestHandler() {
    return workflowSearchRequestHandler;
  }

  @Override
  public WorkflowView getView(Workflow workflow) {
    return workflowViewBuilder.createWorkflowView(workflow);
  }
}
