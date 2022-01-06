/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineServiceConsumersConfig {
  PipelineServiceConsumerConfig interrupt;
  PipelineServiceConsumerConfig orchestrationEvent;
  PipelineServiceConsumerConfig facilitatorEvent;
  PipelineServiceConsumerConfig nodeStart;
  PipelineServiceConsumerConfig progress;
  PipelineServiceConsumerConfig advise;
  PipelineServiceConsumerConfig resume;
  PipelineServiceConsumerConfig sdkResponse;
  PipelineServiceConsumerConfig graphUpdate;
  PipelineServiceConsumerConfig partialPlanResponse;
  PipelineServiceConsumerConfig createPlan;
  PipelineServiceConsumerConfig planNotify;
  PipelineServiceConsumerConfig pmsNotify;
  PipelineServiceConsumerConfig webhookEvent;
}
