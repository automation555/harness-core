/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.rule.Owner;
import io.harness.yaml.schema.JacksonClassHelper;
import io.harness.yaml.schema.beans.SwaggerDefinitionsMetaInfo;

import io.dropwizard.jackson.Jackson;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.DX)
public class JacksonClassHelperTest extends CategoryTest {
  JacksonClassHelper jacksonSubtypeHelper = new JacksonClassHelper(Jackson.newObjectMapper());

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetSubtypeMapping() {
    Map<String, SwaggerDefinitionsMetaInfo> stringModelSet = new HashMap<>();
    jacksonSubtypeHelper.getRequiredMappings(ServiceDefinition.class, stringModelSet);
    assertThat(stringModelSet).isNotEmpty();
    assertThat(stringModelSet.size()).isEqualTo(4);
    assertThat(stringModelSet.get("ClassWhichContainsInterface").getSubtypeClassMap().size()).isEqualTo(1);
    assertThat(stringModelSet.get("testName").getOneOfMappings().size()).isEqualTo(2);
  }
}
