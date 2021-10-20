package io.harness.limits;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;

@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
@Entity(value = "limitCounters", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "CounterKeys")
@HarnessEntity(exportable = true)
@OwnedBy(PL)
@TargetModule(HarnessModule._955_ACCOUNT_MGMT)
public final class Counter extends Base implements AccountAccess {
  @FdUniqueIndex private final String key;
  private final Long value;
  @FdIndex private String accountId;

  public Counter(String key, long value) {
    this.key = key;
    this.value = value;

    populateAccountIdFromKey();
  }

  public void populateAccountIdFromKey() {
    this.accountId = getAccountIdFromKey();
  }

  private String getAccountIdFromKey() {
    Action action = Action.fromKey(key);
    return action.getAccountId();
  }

  // morphia expects an no-args constructor
  private Counter() {
    this.key = null;
    this.value = null;
    this.accountId = null;
  }
}
