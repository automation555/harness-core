package io.harness.connector;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import io.harness.connector.impl.DefaultConnectorServiceImpl;
import io.harness.connector.mappers.ConnectorConfigSummaryDTOMapper;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsConnectorSummaryMapper;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsDTOToEntity;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsEntityToDTO;
import io.harness.connector.mappers.gitconnectormapper.GitConfigSummaryMapper;
import io.harness.connector.mappers.gitconnectormapper.GitDTOToEntity;
import io.harness.connector.mappers.gitconnectormapper.GitEntityToDTO;
import io.harness.connector.mappers.gitconnectormapper.secretmanagermapper.VaultConnectorSummaryDTOMapper;
import io.harness.connector.mappers.gitconnectormapper.secretmanagermapper.VaultDTOToEntity;
import io.harness.connector.mappers.gitconnectormapper.secretmanagermapper.VaultEntityToDTO;
import io.harness.connector.mappers.kubernetesMapper.KubernetesConfigSummaryMapper;
import io.harness.connector.mappers.kubernetesMapper.KubernetesDTOToEntity;
import io.harness.connector.mappers.kubernetesMapper.KubernetesEntityToDTO;
import io.harness.connector.mappers.splunkconnectormapper.SplunkConnectorSummaryMapper;
import io.harness.connector.mappers.splunkconnectormapper.SplunkDTOToEntity;
import io.harness.connector.mappers.splunkconnectormapper.SplunkEntityToDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.validator.AppDynamicsConnectionValidator;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.connector.validator.GitConnectorValidator;
import io.harness.connector.validator.KubernetesConnectionValidator;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.persistence.HPersistence;

public class ConnectorModule extends AbstractModule {
  public static final String DEFAULT_CONNECTOR_SERVICE = "defaultConnectorService";

  @Override
  protected void configure() {
    registerRequiredBindings();

    MapBinder<String, ConnectionValidator> connectorValidatorMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ConnectionValidator.class);
    connectorValidatorMapBinder.addBinding(ConnectorType.KUBERNETES_CLUSTER.getDisplayName())
        .to(KubernetesConnectionValidator.class);
    connectorValidatorMapBinder.addBinding(ConnectorType.GIT.getDisplayName()).to(GitConnectorValidator.class);
    connectorValidatorMapBinder.addBinding(ConnectorType.APP_DYNAMICS.getDisplayName())
        .to(AppDynamicsConnectionValidator.class);

    MapBinder<String, ConnectorDTOToEntityMapper> connectorDTOToEntityMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ConnectorDTOToEntityMapper.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.KUBERNETES_CLUSTER.getDisplayName())
        .to(KubernetesDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.GIT.getDisplayName()).to(GitDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.APP_DYNAMICS.getDisplayName())
        .to(AppDynamicsDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.SPLUNK.getDisplayName()).to(SplunkDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.VAULT.getDisplayName()).to(VaultDTOToEntity.class);

    MapBinder<String, ConnectorEntityToDTOMapper> connectorEntityToDTOMapper =
        MapBinder.newMapBinder(binder(), String.class, ConnectorEntityToDTOMapper.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.KUBERNETES_CLUSTER.getDisplayName())
        .to(KubernetesEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.GIT.getDisplayName()).to(GitEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.APP_DYNAMICS.getDisplayName()).to(AppDynamicsEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.SPLUNK.getDisplayName()).to(SplunkEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.VAULT.getDisplayName()).to(VaultEntityToDTO.class);

    MapBinder<String, ConnectorConfigSummaryDTOMapper> connectorConfigSummaryDTOMapper =
        MapBinder.newMapBinder(binder(), String.class, ConnectorConfigSummaryDTOMapper.class);
    connectorConfigSummaryDTOMapper.addBinding(ConnectorType.KUBERNETES_CLUSTER.getDisplayName())
        .to(KubernetesConfigSummaryMapper.class);
    connectorConfigSummaryDTOMapper.addBinding(ConnectorType.GIT.getDisplayName()).to(GitConfigSummaryMapper.class);
    connectorConfigSummaryDTOMapper.addBinding(ConnectorType.VAULT.getDisplayName())
        .to(VaultConnectorSummaryDTOMapper.class);
    connectorConfigSummaryDTOMapper.addBinding(ConnectorType.APP_DYNAMICS.getDisplayName())
        .to(AppDynamicsConnectorSummaryMapper.class);
    connectorConfigSummaryDTOMapper.addBinding(ConnectorType.SPLUNK.getDisplayName())
        .to(SplunkConnectorSummaryMapper.class);

    bind(ConnectorService.class)
        .annotatedWith(Names.named(DEFAULT_CONNECTOR_SERVICE))
        .to(DefaultConnectorServiceImpl.class);
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
