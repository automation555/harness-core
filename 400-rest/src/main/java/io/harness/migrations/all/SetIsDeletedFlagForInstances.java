package io.harness.migrations.all;

import static io.harness.mongo.MongoUtils.setUnset;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

/**
 * Migration script to set the field isDeleted as false for all active instances.
 *
 * @author rktummala on 10/07/18
 */
@Slf4j
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class SetIsDeletedFlagForInstances implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      log.info("Start - Setting isDeleted flag for active instances");

      List<Key<Instance>> keyList = wingsPersistence.createQuery(Instance.class).asKeyList();
      Set<String> idSet = keyList.stream().map(instanceKey -> (String) instanceKey.getId()).collect(Collectors.toSet());

      UpdateOperations<Instance> updateOperations = wingsPersistence.createUpdateOperations(Instance.class);
      setUnset(updateOperations, "isDeleted", false);
      setUnset(updateOperations, "deletedAt", 0L);

      Query<Instance> query =
          wingsPersistence.createQuery(Instance.class).field("isDeleted").doesNotExist().field("_id").in(idSet);
      wingsPersistence.update(query, updateOperations);

      log.info("End - Setting isDeleted flag for active instances");

    } catch (Exception ex) {
      log.error("Error while setting isDeleted flag for active instances", ex);
    }
  }
}
