package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLGenericFilterType;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAppScopeFilterKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLAppScopeFilter {
  private QLGenericFilterType filterType;
  private String appId;
}
