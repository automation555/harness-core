package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCEEcsEntity {
  String launchType;
  String service;
  String taskId;
}
