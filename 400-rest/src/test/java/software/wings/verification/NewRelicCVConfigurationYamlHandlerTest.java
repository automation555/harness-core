/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.verification;

import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration.NewRelicCVConfigurationYaml;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NewRelicCVConfigurationYamlHandlerTest extends CategoryTest {
  @Mock YamlHelper yamlHelper;
  @Mock CVConfigurationService cvConfigurationService;
  @Mock EnvironmentService environmentService;
  @Mock ServiceResourceService serviceResourceService;
  @Mock AppService appService;
  @Mock SettingsService settingsService;
  @Mock NewRelicService newRelicService;

  private String envId;
  private String serviceId;
  private String appId;
  private String connectorId;
  private String accountId;

  private String appName = "AppName";
  private String serviceName = "serviceName";
  private String connectorName = "newRelicConnector";

  NewRelicCVConfigurationYamlHandler yamlHandler = new NewRelicCVConfigurationYamlHandler();

  @Before
  public void setup() throws IllegalAccessException {
    accountId = generateUUID();
    envId = generateUUID();
    serviceId = generateUUID();
    appId = generateUUID();
    connectorId = generateUUID();

    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(yamlHandler, "yamlHelper", yamlHelper, true);
    FieldUtils.writeField(yamlHandler, "cvConfigurationService", cvConfigurationService, true);
    FieldUtils.writeField(yamlHandler, "appService", appService, true);
    FieldUtils.writeField(yamlHandler, "environmentService", environmentService, true);
    FieldUtils.writeField(yamlHandler, "serviceResourceService", serviceResourceService, true);
    FieldUtils.writeField(yamlHandler, "settingsService", settingsService, true);
    FieldUtils.writeField(yamlHandler, "newRelicService", newRelicService, true);

    Service service = Service.builder().uuid(serviceId).name(serviceName).build();
    when(serviceResourceService.getWithDetails(appId, serviceId)).thenReturn(service);
    when(serviceResourceService.getServiceByName(appId, serviceName)).thenReturn(service);

    long newRelicAppId = 1234;
    when(newRelicService.resolveApplicationId(connectorId, String.valueOf(newRelicAppId)))
        .thenReturn(NewRelicApplication.builder().id(newRelicAppId).name(appName).build());

    when(newRelicService.resolveApplicationName(connectorId, appName))
        .thenReturn(NewRelicApplication.builder().id(newRelicAppId).name(appName).build());

    SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withName(connectorName).withUuid(connectorId).build();
    when(settingsService.getSettingAttributeByName(accountId, connectorName)).thenReturn(settingAttribute);
    when(settingsService.get(connectorId)).thenReturn(settingAttribute);

    Application app = Application.Builder.anApplication().name(generateUUID()).uuid(appId).build();
    when(appService.get(appId)).thenReturn(app);
  }

  private void setBasicInfo(NewRelicCVServiceConfiguration cvServiceConfiguration) {
    cvServiceConfiguration.setStateType(StateType.NEW_RELIC);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setName("TestAppDConfig");
  }

  private NewRelicCVConfigurationYaml buildYaml() {
    NewRelicCVConfigurationYaml yaml = NewRelicCVConfigurationYaml.builder().newRelicApplicationName(appName).build();
    yaml.setServiceName(serviceName);
    yaml.setConnectorName(connectorName);
    yaml.setAlertThreshold(0.5);
    yaml.setNumOfOccurrencesForAlert(3);
    return yaml;
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testToYaml() {
    final String appId = "appId";
    NewRelicCVServiceConfiguration cvServiceConfiguration =
        NewRelicCVServiceConfiguration.builder().applicationId("1234").build();
    setBasicInfo(cvServiceConfiguration);

    NewRelicCVConfigurationYaml yaml = yamlHandler.toYaml(cvServiceConfiguration, appId);

    assertThat(yaml.getServiceName()).isEqualTo(serviceName);
    assertThat(yaml.getNewRelicApplicationName()).isEqualTo(appName);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testToYaml_badApplicationId() {
    final String appId = "appId";
    NewRelicCVServiceConfiguration cvServiceConfiguration =
        NewRelicCVServiceConfiguration.builder().applicationId("12345").build();
    setBasicInfo(cvServiceConfiguration);

    assertThatThrownBy(() -> yamlHandler.toYaml(cvServiceConfiguration, appId))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Invalid NewRelic ApplicationID when converting to YAML: 12345");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpsert() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<NewRelicCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    NewRelicCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertThat(bean.getName()).isEqualTo("TestAppDConfig");
    assertThat(bean.getAppId()).isEqualTo(appId);
    assertThat(bean.getEnvId()).isEqualTo(envId);
    assertThat(bean.getServiceId()).isEqualTo(serviceId);
    assertThat(bean.getUuid()).isNotNull();
    assertThat(bean.getNumOfOccurrencesForAlert()).isEqualTo(3);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpsertBadAppName() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<NewRelicCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    NewRelicCVConfigurationYaml yaml = buildYaml();
    yaml.setNewRelicApplicationName("dummyBadName");
    changeContext.setYaml(yaml);
    assertThatThrownBy(() -> yamlHandler.upsertFromYaml(changeContext, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Invalid NewRelic Application name when saving YAML: dummyBadName");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpsertAlreadyExisting() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    NewRelicCVServiceConfiguration cvConfig = NewRelicCVServiceConfiguration.builder().build();
    cvConfig.setUuid("testUUID");
    when(cvConfigurationService.getConfiguration("TestAppDConfig", appId, envId)).thenReturn(cvConfig);
    ChangeContext<NewRelicCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    NewRelicCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertThat(bean.getName()).isEqualTo("TestAppDConfig");
    assertThat(bean.getUuid()).isEqualTo(cvConfig.getUuid());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testUpsertFromYaml_incorrectThresholdValues() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    NewRelicCVServiceConfiguration cvConfig = NewRelicCVServiceConfiguration.builder().build();
    cvConfig.setUuid("testUUID");
    when(cvConfigurationService.getConfiguration("TestAppDConfig", appId, envId)).thenReturn(cvConfig);
    ChangeContext<NewRelicCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    changeContext.getYaml().setAlertThreshold(1.1);
    assertThatThrownBy(() -> yamlHandler.upsertFromYaml(changeContext, null))
        .isInstanceOf(IllegalArgumentException.class);

    changeContext.getYaml().setAlertThreshold(-0.1);
    assertThatThrownBy(() -> yamlHandler.upsertFromYaml(changeContext, null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
