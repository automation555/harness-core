package io.harness.beans.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HookEventType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class GitWebhookDetails {
  String name;
  String target;
  String secret;
  HookEventType hookEventType;
}
