package io.harness;

import io.harness.grpc.server.PipelineServiceGrpcModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.pms.service.PMSPipelineService;
import io.harness.pms.service.PMSPipelineServiceImpl;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.PipelineServiceModuleRegistrars;
import io.harness.spring.AliasRegistrar;
import io.harness.springdata.SpringPersistenceModule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;

public class PipelineServiceModule extends AbstractModule {
  private static PipelineServiceModule instance;

  public static PipelineServiceModule getInstance() {
    if (instance == null) {
      instance = new PipelineServiceModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(MongoModule.getInstance());
    install(PipelineServiceGrpcModule.getInstance());
    install(new SpringPersistenceModule());

    bind(HPersistence.class).to(MongoPersistence.class);
    bind(PMSPipelineService.class).to(PMSPipelineServiceImpl.class);
  }

  @Provides
  @Singleton
  public Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
        .addAll(PipelineServiceModuleRegistrars.kryoRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
    return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
        .addAll(PipelineServiceModuleRegistrars.morphiaRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends AliasRegistrar>> aliasRegistrars() {
    return ImmutableSet.<Class<? extends AliasRegistrar>>builder()
        .addAll(PipelineServiceModuleRegistrars.aliasRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends TypeConverter>> morphiaConverters() {
    return ImmutableSet.<Class<? extends TypeConverter>>builder()
        .addAll(PipelineServiceModuleRegistrars.morphiaConverters)
        .build();
  }

  @Provides
  @Singleton
  List<Class<? extends Converter<?, ?>>> springConverters() {
    return ImmutableList.<Class<? extends Converter<?, ?>>>builder()
        .addAll(PipelineServiceModuleRegistrars.springConverters)
        .build();
  }

  @Provides
  @Singleton
  public MongoConfig mongoConfig(PipelineServiceConfiguration configuration) {
    return configuration.getMongoConfig();
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  Map<Class, String> morphiaCustomCollectionNames() {
    return Collections.emptyMap();
  }
}
