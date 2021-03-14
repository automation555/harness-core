package io.harness.ccm.cluster.dao;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.cluster.entities.BatchJobInterval;
import io.harness.ccm.cluster.entities.BatchJobInterval.BatchJobIntervalKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@TargetModule(Module._490_CE_COMMONS)
public class BatchJobIntervalDao {
  private final HPersistence hPersistence;

  @Inject
  public BatchJobIntervalDao(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  public BatchJobInterval fetchBatchJobInterval(String accountId, String batchJobType) {
    return hPersistence.createQuery(BatchJobInterval.class)
        .filter(BatchJobIntervalKeys.accountId, accountId)
        .filter(BatchJobIntervalKeys.batchJobType, batchJobType)
        .get();
  }
}
