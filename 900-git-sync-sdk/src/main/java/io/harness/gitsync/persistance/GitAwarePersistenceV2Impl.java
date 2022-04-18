/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.persistance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.v2.GitAware;
import io.harness.gitsync.v2.StoreType;

import com.google.inject.Inject;
import java.util.Optional;

@OwnedBy(HarnessTeam.PL)
public class GitAwarePersistenceV2Impl implements GitAwarePersistenceV2 {
  @Inject GitAwarePersistence gitAwarePersistence;

  @Override
  public <B extends GitAware> Optional<B> findOne(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, Class entityClass, StoreType storeType) {
    if (storeType == null) {
      return gitAwarePersistence.findOne(null, null, null, entityClass);
    }

    // put your logic
    //        return Optional.empty();
  }
}
