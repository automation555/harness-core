package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.AppContainer.AppContainerBuilder.anAppContainer;
import static software.wings.beans.ArtifactSource.ArtifactType.JAR;
import static software.wings.beans.ArtifactSource.ArtifactType.WAR;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.EventType;
import software.wings.beans.Graph;
import software.wings.beans.History;
import software.wings.beans.Notification;
import software.wings.beans.SearchFilter;
import software.wings.beans.Service;
import software.wings.beans.Service.Builder;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnitType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ServiceResourceServiceImpl;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.HistoryService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.stencils.Stencil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 5/4/16.
 */
public class ServiceResourceServiceTest extends WingsBaseTest {
  private static final Command.Builder commandBuilder = aCommand().withName("START").addCommandUnits(
      anExecCommandUnit().withCommandPath("/home/xxx/tomcat").withCommandString("bin/startup.sh").build());
  private static final Builder builder = aService()
                                             .withUuid(SERVICE_ID)
                                             .withAppId(APP_ID)
                                             .withName("SERVICE_NAME")
                                             .withDescription("SERVICE_DESC")
                                             .withArtifactType(JAR)
                                             .withAppContainer(anAppContainer().withUuid("APP_CONTAINER_ID").build());

  @Mock private ServiceTemplateService serviceTemplateService;

  @Inject @Named("primaryDatastore") private Datastore datastore;

  @Mock private WingsPersistence wingsPersistence;

  @Mock private ConfigService configService;
  /**
   * The Verifier.
   */
  @Rule
  public Verifier verifier = new Verifier() {
    @Override
    protected void verify() throws Throwable {
      verifyNoMoreInteractions(configService, wingsPersistence);
    }
  };
  @Mock private AppService appService;
  @Mock private ActivityService activityService;
  @Mock private NotificationService notificationService;
  @Mock private HistoryService historyService;

  @Inject @InjectMocks private ServiceResourceService srs;

  @Spy @InjectMocks private ServiceResourceService spyServiceResourceService = new ServiceResourceServiceImpl();

  @Captor private ArgumentCaptor<Graph> graphArgumentCaptor = ArgumentCaptor.forClass(Graph.class);

  @Captor private ArgumentCaptor<History> historyArgumentCaptor = ArgumentCaptor.forClass(History.class);

  /**
   * Sets the up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.saveAndGet(eq(Service.class), any(Service.class))).thenReturn(builder.but().build());
    when(wingsPersistence.get(eq(Service.class), anyString(), anyString())).thenReturn(builder.but().build());
    when(wingsPersistence.createQuery(Service.class)).thenReturn(datastore.createQuery(Service.class));
    when(wingsPersistence.createUpdateOperations(Service.class))
        .thenReturn(datastore.createUpdateOperations(Service.class));
  }

  /**
   * Should list services.
   */
  @Test
  public void shouldListServices() {
    PageRequest<Service> request = new PageRequest<>();
    request.addFilter("appId", APP_ID, EQ);
    when(wingsPersistence.query(Service.class, request)).thenReturn(new PageResponse<Service>());
    srs.list(request);
    ArgumentCaptor<PageRequest> argument = ArgumentCaptor.forClass(PageRequest.class);
    verify(wingsPersistence).query(eq(Service.class), argument.capture());
    SearchFilter filter = (SearchFilter) argument.getValue().getFilters().get(0);
    assertThat(filter.getFieldName()).isEqualTo("appId");
    assertThat(filter.getFieldValues()).containsExactly(APP_ID);
    assertThat(filter.getOp()).isEqualTo(EQ);
  }

  /**
   * Should save service.
   */
  @Test
  public void shouldSaveService() {
    Service service = builder.but().build();
    doReturn(service).when(spyServiceResourceService).addCommand(any(), any(), any());
    Service savedService = spyServiceResourceService.save(service);

    assertThat(savedService.getUuid()).isEqualTo(SERVICE_ID);
    verify(wingsPersistence).saveAndGet(Service.class, service);
    verify(appService).addService(savedService);
    verify(serviceTemplateService).createDefaultTemplatesByService(savedService);
    verify(spyServiceResourceService, times(3)).addCommand(eq(APP_ID), eq(SERVICE_ID), graphArgumentCaptor.capture());
    verify(notificationService).sendNotificationAsync(any(Notification.class));
    List<Graph> allValues = graphArgumentCaptor.getAllValues();
    assertThat(
        allValues.stream().filter(graph -> asList("Start", "Stop", "Install").contains(graph.getGraphName())).count())
        .isEqualTo(3);
    verify(historyService).createAsync(historyArgumentCaptor.capture());
    assertThat(historyArgumentCaptor.getValue())
        .isNotNull()
        .hasFieldOrPropertyWithValue("eventType", EventType.CREATED)
        .hasFieldOrPropertyWithValue("entityType", EntityType.SERVICE)
        .hasFieldOrPropertyWithValue("entityId", service.getUuid())
        .hasFieldOrPropertyWithValue("entityName", service.getName())
        .hasFieldOrPropertyWithValue("entityNewValue", service);
  }

  /**
   * Should fetch service.
   */
  @Test
  public void shouldGetService() {
    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID)).thenReturn(builder.but().build());
    when(configService.getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID))
        .thenReturn(new ArrayList<ConfigFile>());
    srs.get(APP_ID, SERVICE_ID);
    verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
    verify(activityService).getLastActivityForService(APP_ID, SERVICE_ID);
    verify(activityService).getLastProductionActivityForService(APP_ID, SERVICE_ID);
  }

  /**
   * Should update service.
   */
  @Test
  public void shouldUpdateService() {
    Service service = builder.withName("UPDATED_SERVICE_NAME")
                          .withDescription("UPDATED_SERVICE_DESC")
                          .withArtifactType(WAR)
                          .withAppContainer(anAppContainer().withUuid("UPDATED_APP_CONTAINER_ID").build())
                          .build();
    srs.update(service);
    verify(wingsPersistence)
        .updateFields(Service.class, SERVICE_ID,
            ImmutableMap.of("name", "UPDATED_SERVICE_NAME", "description", "UPDATED_SERVICE_DESC", "artifactType", WAR,
                "appContainer", anAppContainer().withUuid("UPDATED_APP_CONTAINER_ID").build()));
    verify(serviceTemplateService)
        .updateDefaultServiceTemplateName(APP_ID, SERVICE_ID, SERVICE_NAME, "UPDATED_SERVICE_NAME");
    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
  }

  /**
   * Should delete service.
   */
  @Test
  public void shouldDeleteService() {
    when(wingsPersistence.delete(any(), any())).thenReturn(true);
    srs.delete(APP_ID, SERVICE_ID);
    InOrder inOrder = inOrder(wingsPersistence, notificationService, serviceTemplateService, configService);
    inOrder.verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
    inOrder.verify(wingsPersistence).delete(Service.class, SERVICE_ID);
    inOrder.verify(notificationService).sendNotificationAsync(any(Notification.class));
    inOrder.verify(serviceTemplateService).deleteByService(APP_ID, SERVICE_ID);
    inOrder.verify(configService).deleteByEntityId(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
    verify(historyService).createAsync(historyArgumentCaptor.capture());
    assertThat(historyArgumentCaptor.getValue())
        .isNotNull()
        .hasFieldOrPropertyWithValue("eventType", EventType.DELETED)
        .hasFieldOrPropertyWithValue("entityType", EntityType.SERVICE)
        .hasFieldOrPropertyWithValue("entityId", SERVICE_ID)
        .hasFieldOrProperty("entityName")
        .hasFieldOrProperty("entityNewValue");
  }

  /**
   * Should add command state.
   */
  @Test
  public void shouldAddCommand() {
    when(wingsPersistence.addToList(
             eq(Service.class), eq(APP_ID), eq(SERVICE_ID), any(Query.class), eq("commands"), any(Command.class)))
        .thenReturn(true);

    Graph commandGraph = aGraph()
                             .withGraphName("START")
                             .addNodes(aNode()
                                           .withId("1")
                                           .withOrigin(true)
                                           .withType("EXEC")
                                           .addProperty("commandPath", "/home/xxx/tomcat")
                                           .addProperty("command", "bin/startup.sh")
                                           .build())
                             .build();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();

    srs.addCommand(APP_ID, SERVICE_ID, commandGraph);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
    verify(wingsPersistence).save(builder.but().addCommands(expectedCommand).build());
    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  /**
   * Should update command.
   */
  @Test
  public void shouldUpdateCommandWhenCommandChanged() {
    Graph oldCommandGraph = aGraph()
                                .withGraphName("START")
                                .addNodes(aNode()
                                              .withId("1")
                                              .withOrigin(true)
                                              .withType("EXEC")
                                              .addProperty("commandPath", "/home/xxx/tomcat")
                                              .addProperty("commandString", "bin/startup.sh")
                                              .build())
                                .build();

    Command oldCommand = aCommand().withGraph(oldCommandGraph).build();
    oldCommand.transformGraph();
    oldCommand.setVersion(1L);

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(builder.but().addCommands(oldCommand).build());

    Graph commandGraph = aGraph()
                             .withGraphName("START")
                             .addNodes(aNode()
                                           .withId("1")
                                           .withOrigin(true)
                                           .withType("EXEC")
                                           .addProperty("commandPath", "/home/xxx/tomcat1")
                                           .addProperty("commandString", "bin/startup.sh")
                                           .build())
                             .build();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();
    expectedCommand.setVersion(2L);

    srs.updateCommand(APP_ID, SERVICE_ID, commandGraph);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(wingsPersistence).save(builder.but().addCommands(expectedCommand).addOldCommands(oldCommand).build());

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  @Test
  public void shouldUpdateCommandWhenCommandGraphChanged() {
    Graph oldCommandGraph = aGraph()
                                .withGraphName("START")
                                .addNodes(aNode()
                                              .withId("1")
                                              .withOrigin(true)
                                              .withType("EXEC")
                                              .addProperty("commandPath", "/home/xxx/tomcat")
                                              .addProperty("commandString", "bin/startup.sh")
                                              .build())
                                .build();

    Command oldCommand = aCommand().withGraph(oldCommandGraph).build();
    oldCommand.transformGraph();
    oldCommand.setVersion(1L);

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(builder.but().addCommands(oldCommand).build());

    Graph commandGraph = aGraph()
                             .withGraphName("START")
                             .addNodes(aNode()
                                           .withId("1")
                                           .withOrigin(true)
                                           .withType("EXEC")
                                           .withX(1)
                                           .withY(1)
                                           .addProperty("commandPath", "/home/xxx/tomcat")
                                           .addProperty("commandString", "bin/startup.sh")
                                           .build())
                             .build();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();
    expectedCommand.setVersion(2L);

    srs.updateCommand(APP_ID, SERVICE_ID, commandGraph);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(wingsPersistence).save(builder.but().addCommands(expectedCommand).build());

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  @Test
  public void shouldNotUpdateCommandNothingChanged() {
    Graph oldCommandGraph = aGraph()
                                .withGraphName("START")
                                .addNodes(aNode()
                                              .withId("1")
                                              .withOrigin(true)
                                              .withType("EXEC")
                                              .addProperty("commandPath", "/home/xxx/tomcat")
                                              .addProperty("commandString", "bin/startup.sh")
                                              .build())
                                .build();

    Command oldCommand = aCommand().withGraph(oldCommandGraph).build();
    oldCommand.transformGraph();
    oldCommand.setVersion(1L);

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(builder.but().addCommands(oldCommand).build());

    Graph commandGraph = aGraph()
                             .withGraphName("START")
                             .addNodes(aNode()
                                           .withId("1")
                                           .withOrigin(true)
                                           .withType("EXEC")
                                           .addProperty("commandPath", "/home/xxx/tomcat")
                                           .addProperty("commandString", "bin/startup.sh")
                                           .build())
                             .build();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();
    expectedCommand.setVersion(2L);

    srs.updateCommand(APP_ID, SERVICE_ID, commandGraph);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(wingsPersistence, never()).save(builder.but().addCommands(expectedCommand).build());

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  @Test
  public void shouldChangeCommandVersion() {
    Graph oldCommandGraph = aGraph()
                                .withGraphName("START")
                                .addNodes(aNode()
                                              .withId("1")
                                              .withOrigin(true)
                                              .withType("EXEC")
                                              .addProperty("commandPath", "/home/xxx/tomcat")
                                              .addProperty("commandString", "bin/startup.sh")
                                              .build())
                                .build();

    Command oldCommand = aCommand().withGraph(oldCommandGraph).build();
    oldCommand.transformGraph();
    oldCommand.setVersion(1L);

    Graph commandGraph = aGraph()
                             .withGraphName("START")
                             .addNodes(aNode()
                                           .withId("1")
                                           .withOrigin(true)
                                           .withType("EXEC")
                                           .addProperty("commandPath", "/home/xxx/tomcat1")
                                           .addProperty("commandString", "bin/startup.sh")
                                           .build())
                             .build();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();
    expectedCommand.setVersion(2L);

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(builder.but().addCommands(expectedCommand).addOldCommands(oldCommand).build());

    srs.changeCommandVersion(APP_ID, SERVICE_ID, "START", 1);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(wingsPersistence).save(builder.but().addCommands(oldCommand).addOldCommands(expectedCommand).build());

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  @Test
  public void shouldListAllCommandVersions() {
    Graph oldCommandGraph = aGraph()
                                .withGraphName("START")
                                .addNodes(aNode()
                                              .withId("1")
                                              .withOrigin(true)
                                              .withType("EXEC")
                                              .addProperty("commandPath", "/home/xxx/tomcat")
                                              .addProperty("commandString", "bin/startup.sh")
                                              .build())
                                .build();

    Command oldCommand = aCommand().withGraph(oldCommandGraph).build();
    oldCommand.transformGraph();
    oldCommand.setVersion(1L);

    Graph commandGraph = aGraph()
                             .withGraphName("START")
                             .addNodes(aNode()
                                           .withId("1")
                                           .withOrigin(true)
                                           .withType("EXEC")
                                           .addProperty("commandPath", "/home/xxx/tomcat1")
                                           .addProperty("commandString", "bin/startup.sh")
                                           .build())
                             .build();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();
    expectedCommand.setVersion(2L);

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(builder.but().addCommands(expectedCommand).addOldCommands(oldCommand).build());

    List<Command> commands = srs.getCommandVersions(APP_ID, SERVICE_ID, "START");

    assertThat(commands).hasSize(2).containsExactly(expectedCommand, oldCommand);
    verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  /**
   * Should delete command state.
   */
  @Test
  public void shouldDeleteCommand() {
    srs.deleteCommand(APP_ID, SERVICE_ID, "START");

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
    verify(wingsPersistence, times(2)).createUpdateOperations(Service.class);
    verify(wingsPersistence, times(2)).createQuery(Service.class);
    verify(wingsPersistence, times(2)).update(any(Query.class), any());
    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  /**
   * Should get command stencils.
   */
  @Test
  public void shouldGetCommandStencils() {
    when(wingsPersistence.get(eq(Service.class), anyString(), anyString()))
        .thenReturn(
            builder.but().addCommands(commandBuilder.build(), commandBuilder.but().withName("START2").build()).build());

    List<Stencil> commandStencils = srs.getCommandStencils(APP_ID, SERVICE_ID, null);

    assertThat(commandStencils)
        .isNotNull()
        .hasSize(CommandUnitType.values().length + 1)
        .extracting(Stencil::getName)
        .contains("START", "START2");

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
    verify(configService, times(2)).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  /**
   * Should get command by name.
   */
  @Test
  public void shouldGetCommandByName() {
    when(wingsPersistence.get(eq(Service.class), anyString(), anyString()))
        .thenReturn(builder.but().addCommands(commandBuilder.build()).build());

    assertThat(srs.getCommandByName(APP_ID, SERVICE_ID, "START")).isNotNull();

    verify(wingsPersistence, times(1)).get(Service.class, APP_ID, SERVICE_ID);
    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }
}
