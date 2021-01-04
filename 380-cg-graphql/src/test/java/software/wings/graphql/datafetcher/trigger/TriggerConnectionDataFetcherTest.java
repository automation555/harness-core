package software.wings.graphql.datafetcher.trigger;

import static io.harness.rule.OwnerRule.PRABU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.Application;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.query.QLPageQueryParameterImpl;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;
import software.wings.graphql.schema.type.aggregation.trigger.QLTriggerFilter;
import software.wings.graphql.schema.type.aggregation.trigger.QLTriggerTagFilter;
import software.wings.graphql.schema.type.aggregation.trigger.QLTriggerTagType;
import software.wings.graphql.schema.type.trigger.QLTrigger;
import software.wings.graphql.schema.type.trigger.QLTriggerConnection;
import software.wings.security.UserThreadLocal;

import com.google.inject.Inject;
import graphql.execution.MergedSelectionSet;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.SelectedField;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class TriggerConnectionDataFetcherTest extends AbstractDataFetcherTestBase {
  @Mock DataFetchingEnvironment dataFetchingEnvironment;
  @Inject WingsPersistence wingsPersistence;

  @InjectMocks @Inject TriggerConnectionDataFetcher triggerConnectionDataFetcher;
  @InjectMocks @Spy DataFetcherUtils dataFetcherUtils;

  Application app;

  String[] array = new String[1];

  private static final SelectedField selectedField = new SelectedField() {
    @Override
    public String getName() {
      return "total";
    }
    @Override
    public String getQualifiedName() {
      return null;
    }
    @Override
    public GraphQLFieldDefinition getFieldDefinition() {
      return null;
    }
    @Override
    public Map<String, Object> getArguments() {
      return null;
    }
    @Override
    public DataFetchingFieldSelectionSet getSelectionSet() {
      return null;
    }
  };

  private static final DataFetchingFieldSelectionSet mockSelectionSet = new DataFetchingFieldSelectionSet() {
    public MergedSelectionSet get() {
      return MergedSelectionSet.newMergedSelectionSet().build();
    }
    public Map<String, Map<String, Object>> getArguments() {
      return Collections.emptyMap();
    }
    public Map<String, GraphQLFieldDefinition> getDefinitions() {
      return Collections.emptyMap();
    }
    public boolean contains(String fieldGlobPattern) {
      return false;
    }
    public SelectedField getField(String fieldName) {
      return null;
    }
    public List<SelectedField> getFields() {
      return Collections.singletonList(selectedField);
    }
    public List<SelectedField> getFields(String fieldGlobPattern) {
      return Collections.emptyList();
    }
  };

  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    app = createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);
    Workflow workflow = createWorkflow(ACCOUNT1_ID, APP1_ID_ACCOUNT1, WORKLOAD_NAME_ACCOUNT1);
    createCustomTrigger(ACCOUNT1_ID, APP1_ID_ACCOUNT1, TRIGGER_ID1_APP1_ACCOUNT1, "TRIGGER_NAME", workflow.getUuid());
    createCustomTrigger(ACCOUNT1_ID, APP1_ID_ACCOUNT1, TRIGGER_ID2_APP1_ACCOUNT1, "TRIGGER_NAME2", workflow.getUuid());
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testFetchConnection() {
    array[0] = TRIGGER_ID1_APP1_ACCOUNT1;
    QLIdFilter idFilter = QLIdFilter.builder().operator(QLIdOperator.IN).values(array).build();
    QLTagInput tagInput = QLTagInput.builder().name("tag").value("value").build();
    List<QLTagInput> tagInputList = Arrays.asList(tagInput);
    QLTriggerFilter triggerFilter =
        QLTriggerFilter.builder()
            .trigger(idFilter)
            .application(QLIdFilter.builder().operator(QLIdOperator.IN).values(new String[] {APP1_ID_ACCOUNT1}).build())
            .build();
    List<QLTriggerFilter> triggerFilters = Arrays.asList(triggerFilter);
    QLPageQueryParameterImpl pageQueryParams =
        QLPageQueryParameterImpl.builder().limit(100).selectionSet(mockSelectionSet).build();
    QLTriggerConnection connection =
        triggerConnectionDataFetcher.fetchConnection(triggerFilters, pageQueryParams, null);
    assertThat(connection).isNotNull();
    assertThat(connection.getNodes()).hasSize(1);
    assertThat(connection.getNodes().get(0).getName()).isEqualTo("TRIGGER_NAME");

    triggerFilter =
        QLTriggerFilter.builder()
            .tag(QLTriggerTagFilter.builder().tags(tagInputList).entityType(QLTriggerTagType.APPLICATION).build())
            .build();
    connection = triggerConnectionDataFetcher.fetchConnection(Arrays.asList(triggerFilter), pageQueryParams, null);
    assertThat(connection).isNotNull();
    assertThat(connection.getNodes()).isEmpty();
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testFetchConnectionWithEmptyFilters() {
    List<QLTriggerFilter> triggerFilters = Collections.emptyList();
    QLPageQueryParameterImpl pageQueryParams =
        QLPageQueryParameterImpl.builder().limit(100).selectionSet(mockSelectionSet).build();

    QLTriggerConnection connection =
        triggerConnectionDataFetcher.fetchConnection(triggerFilters, pageQueryParams, null);
    assertThat(connection).isNotNull();
    assertThat(connection.getNodes()).hasSize(2);
    assertThat(connection.getNodes().stream().map(QLTrigger::getId))
        .containsExactlyInAnyOrder(TRIGGER_ID1_APP1_ACCOUNT1, TRIGGER_ID2_APP1_ACCOUNT1);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testGenerateFilter() {
    doReturn("fieldValue").when(dataFetcherUtils).getFieldValue(any(), any());
    doReturn("source").when(dataFetchingEnvironment).getSource();
    doReturn("accountid").when(dataFetcherUtils).getAccountId(dataFetchingEnvironment);

    QLTriggerFilter triggerFilter =
        triggerConnectionDataFetcher.generateFilter(dataFetchingEnvironment, "Application", "value");
    assertThat(triggerFilter.getApplication()).isNotNull();
    assertThat(triggerFilter.getApplication().getOperator()).isEqualTo(QLIdOperator.EQUALS);
    assertThat(triggerFilter.getApplication().getValues()).containsExactly("fieldValue");

    triggerFilter = triggerConnectionDataFetcher.generateFilter(dataFetchingEnvironment, "Trigger", "value");
    assertThat(triggerFilter.getTrigger()).isNotNull();
    assertThat(triggerFilter.getTrigger().getOperator()).isEqualTo(QLIdOperator.EQUALS);
    assertThat(triggerFilter.getTrigger().getValues()).containsExactly("fieldValue");
  }
}
