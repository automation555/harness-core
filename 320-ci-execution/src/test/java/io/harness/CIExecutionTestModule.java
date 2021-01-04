package io.harness;

import static org.mockito.Mockito.mock;

import io.harness.ci.beans.entities.LogServiceConfig;
import io.harness.ci.beans.entities.TIServiceConfig;
import io.harness.connector.apis.client.ConnectorResourceClientModule;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.logserviceclient.CILogServiceClientModule;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.secretmanagerclient.SecretManagementClientModule;
import io.harness.secrets.SecretNGManagerClientModule;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.CiExecutionRegistrars;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.OrchestrationBeansRegistrars;
import io.harness.serializer.OrchestrationRegistrars;
import io.harness.serializer.PersistenceRegistrars;
import io.harness.tiserviceclient.TIServiceModule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Set;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;

public class CIExecutionTestModule extends AbstractModule {
  @Provides
  @Singleton
  Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(CiExecutionRegistrars.kryoRegistrars).build();
  }

  @Provides
  @Singleton
  Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
    return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
        .addAll(CiExecutionRegistrars.morphiaRegistrars)
        .build();
  }

  @Provides
  @Singleton
  Set<Class<? extends TypeConverter>> morphiaConverters() {
    return ImmutableSet.<Class<? extends TypeConverter>>builder()
        .addAll(PersistenceRegistrars.morphiaConverters)
        .addAll(OrchestrationBeansRegistrars.morphiaConverters)
        .build();
  }

  @Provides
  @Singleton
  List<Class<? extends Converter<?, ?>>> springConverters() {
    return ImmutableList.<Class<? extends Converter<?, ?>>>builder()
        .addAll(OrchestrationRegistrars.springConverters)
        .build();
  }

  @Provides
  @Singleton
  ServiceTokenGenerator ServiceTokenGenerator() {
    return new ServiceTokenGenerator();
  }

  @Provides
  @Named("serviceSecret")
  String serviceSecret() {
    return "j6ErHMBlC2dn6WctNQKt0xfyo_PZuK7ls0Z4d6XCaBg";
  }

  @Override
  protected void configure() {
    bind(DelegateServiceGrpcClient.class).toInstance(mock(DelegateServiceGrpcClient.class));
    install(new ConnectorResourceClientModule(
        ServiceHttpClientConfig.builder().baseUrl("http://localhost:3457/").build(), "test_secret", "CI"));
    install(new SecretNGManagerClientModule(
        ServiceHttpClientConfig.builder().baseUrl("http://localhost:7457/").build(), "test_secret", "CI"));
    install(new SecretManagementClientModule(
        ServiceHttpClientConfig.builder().baseUrl("http://localhost:3457/").build(), "test_secret", "NextGenManager"));
    install(new CILogServiceClientModule(
        LogServiceConfig.builder().baseUrl("http://localhost:8079").globalToken("token").build()));
    install(new TIServiceModule(TIServiceConfig.builder().baseUrl("http://localhost:8078").build()));
  }
}
