package software.wings.beans.yaml;

import static software.wings.beans.EntityType.VERIFICATION_CONFIGURATION;
import static software.wings.beans.yaml.YamlConstants.ANY;
import static software.wings.beans.yaml.YamlConstants.ANY_EXCEPT_YAML;
import static software.wings.beans.yaml.YamlConstants.APPLICATIONS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.APPLICATION_TEMPLATE_LIBRARY_FOLDER;
import static software.wings.beans.yaml.YamlConstants.ARTIFACT_SERVERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.ARTIFACT_SOURCES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.ARTIFACT_STREAMS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.CLOUD_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.COLLABORATION_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.COMMANDS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.CONFIG_FILES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.CV_CONFIG_FOLDER;
import static software.wings.beans.yaml.YamlConstants.DEFAULTS_YAML;
import static software.wings.beans.yaml.YamlConstants.DEPLOYMENT_SPECIFICATION_FOLDER;
import static software.wings.beans.yaml.YamlConstants.ENVIRONMENTS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.GLOBAL_TEMPLATE_LIBRARY_FOLDER;
import static software.wings.beans.yaml.YamlConstants.HELM_CHART_OVERRIDE_FOLDER;
import static software.wings.beans.yaml.YamlConstants.INDEX_YAML;
import static software.wings.beans.yaml.YamlConstants.INFRA_DEFINITION_FOLDER;
import static software.wings.beans.yaml.YamlConstants.INFRA_MAPPING_FOLDER;
import static software.wings.beans.yaml.YamlConstants.LOAD_BALANCERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FILE_EXPRESSION;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FILE_FOLDER;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FOLDER;
import static software.wings.beans.yaml.YamlConstants.MULTIPLE_ANY;
import static software.wings.beans.yaml.YamlConstants.NOTIFICATION_GROUPS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlConstants.PCF_OVERRIDES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PCF_YAML_EXPRESSION;
import static software.wings.beans.yaml.YamlConstants.PIPELINES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PROVISIONERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SERVICES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER;
import static software.wings.beans.yaml.YamlConstants.TAGS_YAML;
import static software.wings.beans.yaml.YamlConstants.TRIGGER_FOLDER;
import static software.wings.beans.yaml.YamlConstants.VALUES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.VALUES_YAML_KEY;
import static software.wings.beans.yaml.YamlConstants.VERIFICATION_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.WORKFLOWS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.YAML_EXPRESSION;
import static software.wings.utils.Utils.generatePath;

import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.FailureStrategy;
import software.wings.beans.GraphNode;
import software.wings.beans.HarnessTag;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.LambdaSpecification.DefaultSpecification;
import software.wings.beans.LambdaSpecification.FunctionSpecification;
import software.wings.beans.NameValuePair;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.ObjectType;
import software.wings.beans.PhaseStep;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.LogConfiguration;
import software.wings.beans.container.PortMapping;
import software.wings.beans.container.StorageConfiguration;
import software.wings.beans.defaults.Defaults;
import software.wings.beans.template.Template;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.Trigger;
import software.wings.infra.CloudProviderInfrastructure;
import software.wings.infra.InfrastructureDefinition;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.verification.CVConfiguration;
import software.wings.yaml.trigger.ActionYaml;
import software.wings.yaml.trigger.ConditionYaml;
import software.wings.yaml.trigger.PayloadSourceYaml;
import software.wings.yaml.trigger.TriggerArtifactSelectionValueYaml;
import software.wings.yaml.trigger.TriggerArtifactVariableYaml;
import software.wings.yaml.trigger.TriggerConditionYaml;
import software.wings.yaml.trigger.TriggerVariableYaml;
import software.wings.yaml.trigger.WebhookEventYaml;

/**
 * @author rktummala on 10/17/17
 */
public enum YamlType {
  CLOUD_PROVIDER(SettingCategory.CLOUD_PROVIDER.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, CLOUD_PROVIDERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, CLOUD_PROVIDERS_FOLDER, ANY), SettingAttribute.class),
  CLOUD_PROVIDER_OVERRIDE(SettingCategory.CLOUD_PROVIDER.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, CLOUD_PROVIDERS_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, CLOUD_PROVIDERS_FOLDER, ANY), SettingAttribute.class),
  ARTIFACT_SERVER(YamlConstants.ARTIFACT_SERVER,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, ARTIFACT_SERVERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, ARTIFACT_SERVERS_FOLDER, ANY), SettingAttribute.class),
  ARTIFACT_SERVER_OVERRIDE(YamlConstants.ARTIFACT_SERVER,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, ARTIFACT_SERVERS_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, ARTIFACT_SERVERS_FOLDER, ANY), SettingAttribute.class),
  COLLABORATION_PROVIDER(YamlConstants.COLLABORATION_PROVIDER,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, COLLABORATION_PROVIDERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, COLLABORATION_PROVIDERS_FOLDER, ANY), SettingAttribute.class),
  LOADBALANCER_PROVIDER(YamlConstants.LOADBALANCER_PROVIDER,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, LOAD_BALANCERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, LOAD_BALANCERS_FOLDER, ANY), SettingAttribute.class),
  VERIFICATION_PROVIDER(YamlConstants.VERIFICATION_PROVIDER,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, VERIFICATION_PROVIDERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, VERIFICATION_PROVIDERS_FOLDER, ANY), SettingAttribute.class),
  APPLICATION(EntityType.APPLICATION.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY), Application.class),
  SERVICE(EntityType.SERVICE.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY), Service.class),
  APPLICATION_MANIFEST(EntityType.APPLICATION_MANIFEST.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, MANIFEST_FOLDER,
          INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, MANIFEST_FOLDER),
      Service.class),
  APPLICATION_MANIFEST_VALUES_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, VALUES_FOLDER,
          INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, VALUES_FOLDER),
      ApplicationManifest.class),

  // All Services
  APPLICATION_MANIFEST_VALUES_ENV_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          VALUES_FOLDER, INDEX_YAML),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY, VALUES_FOLDER),
      ApplicationManifest.class),
  // Service Specific
  APPLICATION_MANIFEST_VALUES_ENV_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          VALUES_FOLDER, SERVICES_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          VALUES_FOLDER, SERVICES_FOLDER, ANY),
      ApplicationManifest.class),

  // PCF Override All Services
  APPLICATION_MANIFEST_PCF_OVERRIDES_ALL_SERVICE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          PCF_OVERRIDES_FOLDER, INDEX_YAML),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY, PCF_OVERRIDES_FOLDER),
      ApplicationManifest.class),
  // PCF Override Specific Services
  APPLICATION_MANIFEST_PCF_ENV_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          PCF_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          PCF_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY),
      ApplicationManifest.class),
  // Helm Env Service Override
  APPLICATION_MANIFEST_HELM_ENV_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          HELM_CHART_OVERRIDE_FOLDER, SERVICES_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          HELM_CHART_OVERRIDE_FOLDER, SERVICES_FOLDER, ANY),
      ApplicationManifest.class),

  MANIFEST_FILE(YamlConstants.MANIFEST_FILE,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, MANIFEST_FOLDER,
          MANIFEST_FILE_FOLDER, MANIFEST_FILE_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, MANIFEST_FOLDER,
          MANIFEST_FILE_FOLDER),
      ManifestFile.class),
  MANIFEST_FILE_VALUES_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, VALUES_FOLDER,
          VALUES_YAML_KEY),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, VALUES_FOLDER),
      ManifestFile.class),
  MANIFEST_FILE_VALUES_ENV_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          VALUES_FOLDER, VALUES_YAML_KEY),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY, VALUES_FOLDER),
      ManifestFile.class),
  MANIFEST_FILE_VALUES_ENV_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          VALUES_FOLDER, SERVICES_FOLDER, ANY, VALUES_YAML_KEY),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          VALUES_FOLDER, SERVICES_FOLDER, ANY),
      ManifestFile.class),

  // This defines prefix and path expression for PCF Override yml files
  MANIFEST_FILE_PCF_OVERRIDE_ENV_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          PCF_OVERRIDES_FOLDER, PCF_YAML_EXPRESSION),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY, PCF_OVERRIDES_FOLDER),
      ManifestFile.class),
  // This defines prefix and path expression for PCF Override yml files
  MANIFEST_FILE_PCF_OVERRIDE_ENV_SERVICE_OVERRIDE(YamlConstants.VALUES,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          PCF_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY, PCF_YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          PCF_OVERRIDES_FOLDER, SERVICES_FOLDER, ANY),
      ManifestFile.class),
  PROVISIONER(EntityType.PROVISIONER.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, PROVISIONERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, PROVISIONERS_FOLDER, ANY),
      InfrastructureProvisioner.class),
  ARTIFACT_STREAM(EntityType.ARTIFACT_STREAM.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          ARTIFACT_SOURCES_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          ARTIFACT_SOURCES_FOLDER, ANY),
      ArtifactStream.class),
  ARTIFACT_SERVER_ARTIFACT_STREAM_OVERRIDE(EntityType.ARTIFACT_STREAM.name(),
      generatePath(
          PATH_DELIMITER, false, SETUP_FOLDER, ARTIFACT_SERVERS_FOLDER, ANY, ARTIFACT_STREAMS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, ARTIFACT_SERVERS_FOLDER, ANY, ARTIFACT_STREAMS_FOLDER, ANY),
      ArtifactStream.class),
  CLOUD_PROVIDER_ARTIFACT_STREAM_OVERRIDE(EntityType.ARTIFACT_STREAM.name(),
      generatePath(
          PATH_DELIMITER, false, SETUP_FOLDER, CLOUD_PROVIDERS_FOLDER, ANY, ARTIFACT_STREAMS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, CLOUD_PROVIDERS_FOLDER, ANY, ARTIFACT_STREAMS_FOLDER, ANY),
      ArtifactStream.class),
  DEPLOYMENT_SPECIFICATION(YamlConstants.DEPLOYMENT_SPECIFICATION,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          DEPLOYMENT_SPECIFICATION_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          DEPLOYMENT_SPECIFICATION_FOLDER, ANY),
      DefaultSpecification.class),
  COMMAND(EntityType.COMMAND.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, COMMANDS_FOLDER,
          YAML_EXPRESSION),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, COMMANDS_FOLDER, ANY),
      ServiceCommand.class),
  CONFIG_FILE_CONTENT(YamlConstants.CONFIG_FILE_CONTENT,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          CONFIG_FILES_FOLDER, ANY_EXCEPT_YAML),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, CONFIG_FILES_FOLDER, ANY),
      ConfigFile.class),
  CONFIG_FILE(EntityType.CONFIG.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          CONFIG_FILES_FOLDER, YAML_EXPRESSION),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, CONFIG_FILES_FOLDER, ANY),
      ConfigFile.class),
  ENVIRONMENT(EntityType.ENVIRONMENT.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY, INDEX_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY),
      Environment.class),
  CONFIG_FILE_OVERRIDE_CONTENT(YamlConstants.CONFIG_FILE_OVERRIDE_CONTENT,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CONFIG_FILES_FOLDER, ANY_EXCEPT_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CONFIG_FILES_FOLDER, ANY),
      ConfigFile.class),
  CONFIG_FILE_OVERRIDE(EntityType.CONFIG.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CONFIG_FILES_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CONFIG_FILES_FOLDER, ANY),
      ConfigFile.class),
  INFRA_MAPPING(EntityType.INFRASTRUCTURE_MAPPING.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          INFRA_MAPPING_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          INFRA_MAPPING_FOLDER, ANY),
      InfrastructureMapping.class),
  INFRA_DEFINITION(EntityType.INFRASTRUCTURE_DEFINITION.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          INFRA_DEFINITION_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          INFRA_DEFINITION_FOLDER, ANY),
      InfrastructureDefinition.class),
  WORKFLOW(EntityType.WORKFLOW.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, WORKFLOWS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, WORKFLOWS_FOLDER, ANY),
      Workflow.class),
  TRIGGER(EntityType.TRIGGER.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, TRIGGER_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, TRIGGER_FOLDER, ANY), Trigger.class),
  DEPLOYMENT_TRIGGER(EntityType.DEPLOYMENT_TRIGGER.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, TRIGGER_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, TRIGGER_FOLDER, ANY),
      DeploymentTrigger.class),

  PIPELINE(EntityType.PIPELINE.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, PIPELINES_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, PIPELINES_FOLDER, ANY),
      Pipeline.class),

  GLOBAL_TEMPLATE_LIBRARY(EntityType.TEMPLATE.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, GLOBAL_TEMPLATE_LIBRARY_FOLDER, MULTIPLE_ANY, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, GLOBAL_TEMPLATE_LIBRARY_FOLDER), Template.class),

  APPLICATION_TEMPLATE_LIBRARY(EntityType.TEMPLATE.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, APPLICATION_TEMPLATE_LIBRARY_FOLDER,
          MULTIPLE_ANY, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, APPLICATION_TEMPLATE_LIBRARY_FOLDER),
      Template.class),

  CV_CONFIGURATION(VERIFICATION_CONFIGURATION.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CV_CONFIG_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CV_CONFIG_FOLDER, ANY),
      CVConfiguration.class),

  // Some of these bean classes are embedded within other entities and don't have an yaml file path
  NAME_VALUE_PAIR(ObjectType.NAME_VALUE_PAIR, "", "", NameValuePair.class),
  PHASE(ObjectType.PHASE, "", "", WorkflowPhase.class),
  PHASE_STEP(ObjectType.PHASE_STEP, "", "", PhaseStep.class),
  TEMPLATE_EXPRESSION(ObjectType.TEMPLATE_EXPRESSION, "", "", TemplateExpression.class),
  VARIABLE(ObjectType.VARIABLE, "", "", Variable.class),
  STEP(ObjectType.STEP, "", "", GraphNode.class),
  FAILURE_STRATEGY(ObjectType.FAILURE_STRATEGY, "", "", FailureStrategy.class),
  NOTIFICATION_RULE(ObjectType.NOTIFICATION_RULE, "", "", NotificationRule.class),
  PIPELINE_STAGE(ObjectType.PIPELINE_STAGE, "", "", PipelineStage.class),
  NOTIFICATION_GROUP(ObjectType.NOTIFICATION_GROUP,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, NOTIFICATION_GROUPS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, NOTIFICATION_GROUPS_FOLDER, ANY), NotificationGroup.class),
  COMMAND_UNIT(ObjectType.COMMAND_UNIT, "", "", AbstractCommandUnit.class),
  CONTAINER_DEFINITION(ObjectType.CONTAINER_DEFINITION, "", "", ContainerDefinition.class),
  LOG_CONFIGURATION(ObjectType.LOG_CONFIGURATION, "", "", LogConfiguration.class),
  PORT_MAPPING(ObjectType.PORT_MAPPING, "", "", PortMapping.class),
  STORAGE_CONFIGURATION(ObjectType.STORAGE_CONFIGURATION, "", "", StorageConfiguration.class),
  DEFAULT_SPECIFICATION(ObjectType.DEFAULT_SPECIFICATION, "", "", DefaultSpecification.class),
  FUNCTION_SPECIFICATION(ObjectType.FUNCTION_SPECIFICATION, "", "", FunctionSpecification.class),
  SETTING_ATTRIBUTE(ObjectType.SETTING_ATTRIBUTE, "", "", SettingAttribute.class),
  SETTING_VALUE(ObjectType.SETTING_VALUE, "", "", SettingValue.class),
  APPLICATION_DEFAULTS(ObjectType.APPLICATION_DEFAULTS,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, DEFAULTS_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY), Defaults.class),
  ACCOUNT_DEFAULTS(ObjectType.ACCOUNT_DEFAULTS, generatePath(PATH_DELIMITER, false, SETUP_FOLDER, DEFAULTS_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, ANY), Defaults.class),
  USAGE_RESTRICTIONS(ObjectType.USAGE_RESTRICTIONS, "", "", UsageRestrictions.class),
  TRIGGER_CONDITION(ObjectType.TRIGGER_CONDITION, "", "", TriggerConditionYaml.class),
  CONDITION(ObjectType.CONDITION, "", "", ConditionYaml.class),
  PAYLOAD_SOURCE(ObjectType.PAYLOAD_SOURCE, "", "", PayloadSourceYaml.class),
  ACTION(ObjectType.ACTION, "", "", ActionYaml.class),
  TRIGGER_ARTIFACT_VARIABLE(ObjectType.TRIGGER_ARTIFACT_VARIABLE, "", "", TriggerArtifactVariableYaml.class),
  WEBHOOK_EVENT(ObjectType.WEBHOOK_EVENT, "", "", WebhookEventYaml.class),
  TRIGGER_VARIABLE(ObjectType.TRIGGER_VARIABLE, "", "", TriggerVariableYaml.class),
  TRIGGER_ARTIFACT_VALUE(ObjectType.TRIGGER_ARTIFACT_VALUE, "", "", TriggerArtifactSelectionValueYaml.class),
  ARTIFACT_SELECTION(ObjectType.ARTIFACT_SELECTION, "", "", ArtifactSelection.Yaml.class),
  TAG(EntityType.TAG.name(), generatePath(PATH_DELIMITER, false, SETUP_FOLDER, TAGS_YAML),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, ANY), HarnessTag.class),
  CLOUD_PROVIDER_INFRASTRUCTURE(ObjectType.CLOUD_PROVIDER_INFRASTRUCTURE, "", "", CloudProviderInfrastructure.class);

  private String entityType;
  private String pathExpression;
  private String prefixExpression;
  private Class beanClass;

  YamlType(String entityType, String pathExpression, String prefixExpression, Class beanClass) {
    this.entityType = entityType;
    this.pathExpression = pathExpression;
    this.prefixExpression = prefixExpression;
    this.beanClass = beanClass;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getPathExpression() {
    return pathExpression;
  }

  public String getPrefixExpression() {
    return prefixExpression;
  }

  public Class getBeanClass() {
    return beanClass;
  }
}
