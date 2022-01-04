package software.wings.infra;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.expression.Expression.DISALLOW_SECRETS;
import static io.harness.validation.Validator.ensureType;

import static software.wings.beans.InfrastructureType.RANCHER_KUBERNETES;
import static software.wings.beans.RancherKubernetesInfrastructureMapping.Builder.aRancherKubernetesInfrastructureMapping;
import static software.wings.common.InfrastructureConstants.INFRA_KUBERNETES_INFRAID_EXPRESSION;

import static java.lang.String.format;

import io.harness.data.validator.Trimmed;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.Expression;

import software.wings.annotation.IncludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.RancherKubernetesInfrastructureMapping;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

@JsonTypeName("RANCHER_KUBERNETES")
@Data
@Builder
@FieldNameConstants(innerTypeName = "RancherKubernetesInfrastructureKeys")
public class RancherKubernetesInfrastructure
    implements InfraMappingInfrastructureProvider, KubernetesInfrastructure, FieldKeyValMapProvider, ProvisionerAware {
  private String cloudProviderId;
  @IncludeFieldMap private String clusterName;
  @IncludeFieldMap @Expression(DISALLOW_SECRETS) private String namespace;
  @Trimmed private String releaseName;
  private List<ClusterSelectionCriteriaEntry> clusterSelectionCriteria;

  @Data
  @Builder
  public static class ClusterSelectionCriteriaEntry {
    String labelName;
    String labelValues;
  }

  private Map<String, String> expressions;

  @Override
  public InfrastructureMapping getInfraMapping() {
    return aRancherKubernetesInfrastructureMapping()
        .withClusterName(clusterName)
        .withNamespace(namespace)
        .withReleaseName(releaseName)
        .withComputeProviderSettingId(cloudProviderId)
        .withInfraMappingType(InfrastructureMappingType.RANCHER_KUBERNETES.name())
        .build();
  }

  @Override
  public String getReleaseName() {
    return isEmpty(releaseName) ? INFRA_KUBERNETES_INFRAID_EXPRESSION : releaseName;
  }

  @Override
  public Class<RancherKubernetesInfrastructureMapping> getMappingClass() {
    return RancherKubernetesInfrastructureMapping.class;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.RANCHER;
  }

  @Override
  public String getInfrastructureType() {
    return RANCHER_KUBERNETES;
  }

  @Override
  public Set<String> getSupportedExpressions() {
    return ImmutableSet.of(RancherKubernetesInfrastructure.RancherKubernetesInfrastructureKeys.namespace,
        RancherKubernetesInfrastructure.RancherKubernetesInfrastructureKeys.releaseName);
  }

  @Override
  public void applyExpressions(
      Map<String, Object> resolvedExpressions, String appId, String envId, String infraDefinitionId) {
    for (Map.Entry<String, Object> entry : resolvedExpressions.entrySet()) {
      switch (entry.getKey()) {
        case "namespace":
          ensureType(String.class, entry.getValue(), "Namespace should be of String type");
          setNamespace((String) entry.getValue());
          break;
        case "releaseName":
          ensureType(String.class, entry.getValue(), "Release name should be of String type");
          setReleaseName((String) entry.getValue());
          break;
        default:
          throw new InvalidRequestException(format("Unknown expression : [%s]", entry.getKey()));
      }
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(RANCHER_KUBERNETES)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
    private String cloudProviderName;
    private String clusterName;
    private String namespace;
    private String releaseName;
    private List<ClusterSelectionCriteriaEntry> clusterSelectionCriteria;

    private Map<String, String> expressions;

    @Builder
    public Yaml(String type, String cloudProviderName, String clusterName, String namespace, String releaseName,
        List<ClusterSelectionCriteriaEntry> clusterSelectionCriteria, Map<String, String> expressions) {
      super(type);
      setCloudProviderName(cloudProviderName);
      setClusterName(clusterName);
      setNamespace(namespace);
      setReleaseName(releaseName);
      setExpressions(expressions);
      setClusterSelectionCriteria(clusterSelectionCriteria);
    }

    public Yaml() {
      super(RANCHER_KUBERNETES);
    }
  }
}
