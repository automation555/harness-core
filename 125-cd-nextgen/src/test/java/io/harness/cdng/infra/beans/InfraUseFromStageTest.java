package io.harness.cdng.infra.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ACHYUTH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.InfrastructureDef;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
@OwnedBy(CDP)
public class InfraUseFromStageTest extends CategoryTest {
  private InfraUseFromStage infraUseFromStage = InfraUseFromStage.builder().build();
  private InfraUseFromStage.Overrides overrides = new InfraUseFromStage.Overrides(
      EnvironmentYaml.builder().build(), InfrastructureDef.builder().build(), "metaData");
  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testVisitableChildren() {
    assertThat(infraUseFromStage.getChildrenToWalk().getVisitableChildList()).isNotNull();

    assertThat(overrides.getChildrenToWalk().getVisitableChildList()).isNotNull();
  }
}