package io.harness.grpc.client;

import io.harness.govern.ProviderModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public abstract class AbstractManagerGrpcClientModule extends ProviderModule {
  @Override
  protected void configure() {
    install(ManagerGrpcClientModule.getInstance());
  }

  @Provides
  @Singleton
  protected ManagerGrpcClientModule.Config injectConfig() {
    return config();
  }

  public abstract ManagerGrpcClientModule.Config config();

  @Provides
  @Singleton
  @Named("Application")
  protected String injectApplication() {
    return application();
  }

  public abstract String application();
}
