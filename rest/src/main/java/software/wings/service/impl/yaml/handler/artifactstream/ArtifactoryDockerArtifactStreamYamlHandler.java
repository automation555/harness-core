package software.wings.service.impl.yaml.handler.artifactstream;

import com.google.inject.Singleton;

import software.wings.beans.artifact.ArtifactoryDockerArtifactStream;
import software.wings.beans.artifact.ArtifactoryDockerArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

/**
 * @author rktummala on 10/09/17
 */
@Singleton
public class ArtifactoryDockerArtifactStreamYamlHandler
    extends ArtifactStreamYamlHandler<Yaml, ArtifactoryDockerArtifactStream> {
  @Override
  public Yaml toYaml(ArtifactoryDockerArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setDockerImageName(bean.getGroupId());
    yaml.setImageName(bean.getImageName());
    yaml.setMetadataOnly(bean.isMetadataOnly());
    yaml.setRepositoryName(bean.getJobname());
    return yaml;
  }

  protected void toBean(ArtifactoryDockerArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setImageName(yaml.getImageName());
    bean.setJobname(yaml.getRepositoryName());
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected ArtifactoryDockerArtifactStream getNewArtifactStreamObject() {
    return new ArtifactoryDockerArtifactStream();
  }
}
