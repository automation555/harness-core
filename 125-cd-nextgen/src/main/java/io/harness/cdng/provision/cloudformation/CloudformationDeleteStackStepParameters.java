/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.cloudformation;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@RecasterAlias("io.harness.cdng.provision.cloudformation.CloudformationDeleteStackStepParameters")
public class CloudformationDeleteStackStepParameters
    extends CloudformationDeleteStackBaseStepInfo implements SpecParameters {
  @NonNull CloudformationDeleteStackStepConfiguration configuration;

  @Builder(builderMethodName = "infoBuilder")
  public CloudformationDeleteStackStepParameters(ParameterField<List<TaskSelectorYaml>> delegateSelectors,
      @NonNull CloudformationDeleteStackStepConfiguration configuration, String uuid) {
    super(delegateSelectors, uuid);
    this.configuration = configuration;
  }
}
