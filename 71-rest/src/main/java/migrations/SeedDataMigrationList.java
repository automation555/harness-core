package migrations;

import com.google.common.collect.ImmutableList;

import migrations.seedata.IISInstallCommandMigration;
import migrations.seedata.IISInstallCommandV4Migration;
import migrations.seedata.TemplateGalleryDefaultTemplatesMigration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class SeedDataMigrationList {
  public static List<Pair<Integer, Class<? extends SeedDataMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends SeedDataMigration>>>()
        .add(Pair.of(1, BaseSeedDataMigration.class))
        .add(Pair.of(2, BaseSeedDataMigration.class))
        .add(Pair.of(3, TemplateGalleryDefaultTemplatesMigration.class))
        .add(Pair.of(4, IISInstallCommandMigration.class))
        .add(Pair.of(5, BaseSeedDataMigration.class))
        .add(Pair.of(6, BaseSeedDataMigration.class))
        .add(Pair.of(7, BaseSeedDataMigration.class))
        .add(Pair.of(8, IISInstallCommandV4Migration.class))
        .build();
  }
}
