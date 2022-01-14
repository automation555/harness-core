package software.wings.graphql.schema.type;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.artifact.QLArtifact;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@Scope(ResourceType.APPLICATION)
@OwnedBy(HarnessTeam.CDC)
public class QLArtifactConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLArtifact> nodes;
}
