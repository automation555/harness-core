package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.beans.ServiceVariable.Type.TEXT;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_VARIABLE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_VARIABLE_NAME;
import static software.wings.utils.WingsTestConstants.TARGET_APP_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.yaml.YamlDirectoryService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 8/9/16.
 */
public class ServiceVariableServiceTest extends WingsBaseTest {
  private static final ServiceVariable SERVICE_VARIABLE = ServiceVariable.builder()
                                                              .envId(ENV_ID)
                                                              .entityType(EntityType.SERVICE_TEMPLATE)
                                                              .entityId(TEMPLATE_ID)
                                                              .templateId(TEMPLATE_ID)
                                                              .name(SERVICE_VARIABLE_NAME)
                                                              .type(TEXT)
                                                              .value("8080".toCharArray())
                                                              .build();

  private static final ServiceVariable ENCRYPTED_SERVICE_VARIABLE = ServiceVariable.builder()
                                                                        .envId(ENV_ID)
                                                                        .entityType(EntityType.SERVICE_TEMPLATE)
                                                                        .entityId(TEMPLATE_ID)
                                                                        .templateId(TEMPLATE_ID)
                                                                        .name(SERVICE_VARIABLE_NAME + "2")
                                                                        .type(ENCRYPTED_TEXT)
                                                                        .value("9090".toCharArray())
                                                                        .build();

  static {
    SERVICE_VARIABLE.setUuid(SERVICE_VARIABLE_ID);
    SERVICE_VARIABLE.setAppId(APP_ID);

    ENCRYPTED_SERVICE_VARIABLE.setUuid(SERVICE_VARIABLE_ID + "2");
    ENCRYPTED_SERVICE_VARIABLE.setAppId(APP_ID);
  }
  /**
   * The Query.
   */
  @Mock Query<ServiceVariable> query;
  /**
   * The End.
   */
  @Mock FieldEnd end;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private AppService appService;
  @Mock private YamlDirectoryService yamlDirectoryService;
  @Inject @InjectMocks private ServiceVariableService serviceVariableService;

  /**
   * Sets up.
   *
   * @throws IOException the io exception
   */
  @Before
  public void setUp() throws IOException {
    when(wingsPersistence.createQuery(ServiceVariable.class)).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
    when(appService.get(TARGET_APP_ID))
        .thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
    when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
  }

  /**
   * Should list.
   */
  @Test
  public void shouldList() {
    PageResponse<ServiceVariable> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(SERVICE_VARIABLE));
    pageResponse.setTotal(1);

    PageRequest pageRequest = aPageRequest()
                                  .withLimit("50")
                                  .withOffset("0")
                                  .addFilter(aSearchFilter()
                                                 .withField("appId", EQ, APP_ID)
                                                 .withField("envId", EQ, ENV_ID)
                                                 .withField("templateId", EQ, TEMPLATE_ID)
                                                 .withField("entityId", EQ, "ENTITY_ID")
                                                 .build())
                                  .build();

    when(wingsPersistence.query(ServiceVariable.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<ServiceVariable> configserviceSettingsiles = serviceVariableService.list(pageRequest);
    assertThat(configserviceSettingsiles).isNotNull();
    assertThat(configserviceSettingsiles.getResponse().get(0)).isInstanceOf(ServiceVariable.class);
  }

  /**
   * Should save.
   */
  @Test
  public void shouldSave() {
    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID))
        .thenReturn(aServiceTemplate().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TEMPLATE_ID).build());
    serviceVariableService.save(SERVICE_VARIABLE);
    verify(serviceTemplateService).get(APP_ID, TEMPLATE_ID);
    verify(wingsPersistence).saveAndGet(ServiceVariable.class, SERVICE_VARIABLE);
  }

  /**
   * Should throw exception for unsupported entity types.
   */
  @Test
  public void shouldThrowExceptionForUnsupportedEntityTypes() {
    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .envId(ENV_ID)
                                          .entityType(EntityType.APPLICATION)
                                          .entityId(TEMPLATE_ID)
                                          .templateId(TEMPLATE_ID)
                                          .type(TEXT)
                                          .value("8080".toCharArray())
                                          .build();
    serviceVariable.setAppId(APP_ID);
    serviceVariable.setUuid(SERVICE_VARIABLE_ID);

    Assertions.assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> serviceVariableService.save(serviceVariable));
  }

  /**
   * Should get.
   */
  @Test
  public void shouldGet() {
    ServiceVariable variable = ServiceVariable.builder().build();
    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);
    when(wingsPersistence.get(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
    ServiceVariable serviceVariable = serviceVariableService.get(APP_ID, SERVICE_VARIABLE_ID);
    verify(wingsPersistence).get(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID);
    assertThat(serviceVariable.getUuid()).isEqualTo(SERVICE_VARIABLE_ID);
  }

  /**
   * Should get by template.
   */
  @Test
  public void shouldGetByTemplate() {
    ServiceTemplate serviceTemplate =
        aServiceTemplate().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TEMPLATE_ID).build();

    PageRequest<ServiceVariable> request =
        aPageRequest()
            .addFilter(aSearchFilter().withField("appId", Operator.EQ, APP_ID).build())
            .addFilter(aSearchFilter().withField("envId", Operator.EQ, ENV_ID).build())
            .addFilter(aSearchFilter().withField("templateId", Operator.EQ, TEMPLATE_ID).build())
            .build();
    PageResponse<ServiceVariable> resp = new PageResponse<>();
    resp.setResponse(Arrays.asList(SERVICE_VARIABLE));
    when(wingsPersistence.query(ServiceVariable.class, request)).thenReturn(resp);
    when(wingsPersistence.get(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(SERVICE_VARIABLE);
    List<ServiceVariable> serviceVariables =
        serviceVariableService.getServiceVariablesByTemplate(APP_ID, ENV_ID, serviceTemplate, false);

    verify(wingsPersistence).query(ServiceVariable.class, request);
    assertThat(serviceVariables.get(0)).isEqualTo(SERVICE_VARIABLE);
  }

  /**
   * Should update.
   */
  @Test
  public void shouldUpdate() {
    ServiceVariable variable = ServiceVariable.builder().entityType(EntityType.SERVICE_TEMPLATE).build();
    variable.setAppId(APP_ID);
    variable.setUuid(SERVICE_VARIABLE_ID);

    when(wingsPersistence.get(ServiceVariable.class, APP_ID, SERVICE_VARIABLE_ID)).thenReturn(variable);
    serviceVariableService.update(SERVICE_VARIABLE);
    verify(wingsPersistence)
        .updateFields(ServiceVariable.class, SERVICE_VARIABLE_ID,
            ImmutableMap.of("value", SERVICE_VARIABLE.getValue(), "type", TEXT));
  }

  /**
   * Should delete.
   */
  @Test
  public void shouldDelete() {
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);
    when(wingsPersistence.createQuery(ServiceVariable.class)).thenReturn(query);
    serviceVariableService.delete(APP_ID, SERVICE_VARIABLE_ID);
    verify(wingsPersistence).delete(query);
  }

  /**
   * Should get for entity.
   */
  @Test
  public void shouldGetForEntity() {
    PageResponse<ServiceVariable> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(SERVICE_VARIABLE));
    pageResponse.setTotal(1);

    PageRequest<ServiceVariable> pageRequest =
        aPageRequest()
            .addFilter(aSearchFilter().withField("appId", Operator.EQ, APP_ID).build())
            .addFilter(aSearchFilter().withField("entityId", Operator.EQ, "ENTITY_ID").build())
            .build();

    when(wingsPersistence.query(ServiceVariable.class, pageRequest)).thenReturn(pageResponse);

    serviceVariableService.getServiceVariablesForEntity(APP_ID, TEMPLATE_ID, "ENTITY_ID", false);
    verify(wingsPersistence).query(ServiceVariable.class, pageRequest);
  }

  /**
   * Should delete by entity id.
   */
  @Test
  public void shouldDeleteByEntityId() {
    PageResponse<ServiceVariable> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(SERVICE_VARIABLE));
    pageResponse.setTotal(1);

    PageRequest<ServiceVariable> pageRequest =
        aPageRequest()
            .addFilter(aSearchFilter().withField("appId", Operator.EQ, APP_ID).build())
            .addFilter(aSearchFilter().withField("entityId", Operator.EQ, "ENTITY_ID").build())
            .build();

    when(wingsPersistence.createQuery(ServiceVariable.class)).thenReturn(query);
    when(wingsPersistence.query(ServiceVariable.class, pageRequest)).thenReturn(pageResponse);
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);

    serviceVariableService.deleteByEntityId(APP_ID, TEMPLATE_ID, "ENTITY_ID");
    verify(wingsPersistence).delete(query);
  }

  /**
   * Should mask encrypted fields.
   */
  @Test
  public void shouldMaskEncryptedFields() {
    PageResponse<ServiceVariable> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(ENCRYPTED_SERVICE_VARIABLE));
    pageResponse.setTotal(1);

    PageRequest pageRequest = aPageRequest()
                                  .withLimit("50")
                                  .withOffset("0")
                                  .addFilter(aSearchFilter()
                                                 .withField("appId", EQ, APP_ID)
                                                 .withField("envId", EQ, ENV_ID)
                                                 .withField("templateId", EQ, TEMPLATE_ID)
                                                 .withField("entityId", EQ, "ENTITY_ID")
                                                 .build())
                                  .build();

    when(wingsPersistence.query(ServiceVariable.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<ServiceVariable> serviceVariablePageResponse = serviceVariableService.list(pageRequest);
    assertThat(serviceVariablePageResponse).isNotNull();
    assertThat(serviceVariablePageResponse.getResponse().get(0)).isInstanceOf(ServiceVariable.class);
    assertThat(Arrays.equals(serviceVariablePageResponse.getResponse().get(0).getValue(), "******".toCharArray()));
  }
}
