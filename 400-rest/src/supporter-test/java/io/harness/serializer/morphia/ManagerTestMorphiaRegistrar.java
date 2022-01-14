package io.harness.serializer.morphia;

import io.harness.engine.interrupts.steps.TestTransportEntity;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import software.wings.integration.common.MongoEntity;
import software.wings.integration.dl.Dummy;

import java.util.Set;

public class ManagerTestMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Dummy.class);
    set.add(MongoEntity.class);
    set.add(TestTransportEntity.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // nothing to registrer
  }
}
