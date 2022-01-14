package software.wings.graphql.schema.type.aggregation.cv;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLVerificationResultFilter implements EntityFilter {
  QLIdFilter application;
  QLIdFilter service;
  QLIdFilter environment;
  QLCVWorkflowTagFilter tag;
  Boolean rollback;
  QLVerificationTypeFilter type;
  QLTimeFilter startTime;
  QLTimeFilter endTime;
}
