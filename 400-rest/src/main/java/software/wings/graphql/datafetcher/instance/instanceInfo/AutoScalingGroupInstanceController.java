/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.instance.instanceInfo;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.graphql.datafetcher.instance.InstanceControllerUtils;
import software.wings.graphql.schema.type.instance.QLAutoScalingGroupInstance;
import software.wings.graphql.schema.type.instance.QLInstanceType;

import com.google.inject.Inject;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class AutoScalingGroupInstanceController implements InstanceController<QLAutoScalingGroupInstance> {
  @Inject InstanceControllerUtils util;

  @Override
  public QLAutoScalingGroupInstance populateInstance(Instance instance) {
    AutoScalingGroupInstanceInfo info = (AutoScalingGroupInstanceInfo) instance.getInstanceInfo();
    return QLAutoScalingGroupInstance.builder()
        .id(instance.getUuid())
        .applicationId(instance.getAppId())
        .environmentId(instance.getEnvId())
        .serviceId(instance.getServiceId())
        .artifact(util.getQlArtifact(instance))
        .type(QLInstanceType.AUTOSCALING_GROUP_INSTANCE)
        .autoScalingGroupName(info.getAutoScalingGroupName())
        .hostPublicDns(info.getHostPublicDns())
        .hostId(info.getHostId())
        .hostName(info.getHostName())
        .build();
  }
}
