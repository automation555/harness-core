package io.harness.functional.migration;

import static io.harness.rule.OwnerRule.ANKIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import io.harness.category.element.FunctionalTests;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.AccountType;
import software.wings.features.GitOpsFeature;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AccountMigrationToEssentialsTest extends AbstractAccountMigrationTest {
  @Test
  @Owner(emails = ANKIT)
  @Category(FunctionalTests.class)
  public void testMigrateAlreadyCompliantTrialAccountToEssentials() {
    updateAccountLicense(AccountType.ESSENTIALS);

    assertEquals(AccountType.ESSENTIALS, getAccountType());
  }

  @Test
  @Owner(emails = ANKIT)
  @Category(FunctionalTests.class)
  public void testMigrateNonCompliantTrialAccountToEssentials() {
    makeAccountNonCompliant();

    updateAccountLicense(AccountType.ESSENTIALS);

    assertNotEquals(AccountType.ESSENTIALS, getAccountType());

    Map<String, Object> gitOpsComplyInfo = new HashMap<>();
    gitOpsComplyInfo.put("sourceReposToRetain", Arrays.asList(Settings.TERRAFORM_MAIN_GIT_REPO.name()));

    Map<String, Map<String, Object>> requiredInfoToComply = new HashMap<>();
    requiredInfoToComply.put(GitOpsFeature.FEATURE_NAME, gitOpsComplyInfo);

    updateAccountLicense(AccountType.ESSENTIALS, requiredInfoToComply);

    assertEquals(AccountType.ESSENTIALS, getAccountType());
  }

  private void makeAccountNonCompliant() {
    addSourceRepos();

    addWhitelistedIP();
    addApiKey();

    addWorkflowWithJira();
  }
}
