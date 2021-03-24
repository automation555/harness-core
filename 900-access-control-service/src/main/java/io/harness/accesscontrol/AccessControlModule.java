package io.harness.accesscontrol;

import static io.harness.AuthorizationServiceHeader.ACCESS_CONTROL_SERVICE;
import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.ACCOUNT;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.ORGANIZATION;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.PROJECT;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;

import io.harness.AccessControlClientModule;
import io.harness.DecisionModule;
import io.harness.accesscontrol.commons.iterators.AccessControlIteratorsConfig;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.principals.PrincipalValidator;
import io.harness.accesscontrol.principals.user.UserValidator;
import io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupService;
import io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupServiceImpl;
import io.harness.accesscontrol.resources.resourcegroups.events.ResourceGroupEventConsumer;
import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.core.ScopeParamsFactory;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParamsFactory;
import io.harness.aggregator.AggregatorModule;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.EventConsumer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.ng.core.UserClientModule;
import io.harness.redis.RedisConfig;
import io.harness.resourcegroupclient.ResourceGroupClientModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import ru.vyarus.guice.validator.ValidationModule;

public class AccessControlModule extends AbstractModule {
  private static AccessControlModule instance;
  private final AccessControlConfiguration config;

  private AccessControlModule(AccessControlConfiguration config) {
    this.config = config;
  }

  public static synchronized AccessControlModule getInstance(AccessControlConfiguration config) {
    if (instance == null) {
      instance = new AccessControlModule(config);
    }
    return instance;
  }

  @Provides
  @Named(ENTITY_CRUD)
  public Consumer getConsumer() {
    RedisConfig redisConfig = config.getEventsConfig().getRedisConfig();
    if (!config.getEventsConfig().isEnabled()) {
      return NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME);
    }
    return RedisConsumer.of(ENTITY_CRUD, ACCESS_CONTROL_SERVICE.getServiceId(), redisConfig,
        EventsFrameworkConstants.ENTITY_CRUD_MAX_PROCESSING_TIME, EventsFrameworkConstants.ENTITY_CRUD_READ_BATCH_SIZE);
  }

  @Provides
  public AccessControlIteratorsConfig getIteratorsConfig() {
    return config.getIteratorsConfig();
  }

  @Override
  protected void configure() {
    install(AccessControlPersistenceModule.getInstance(config.getMongoConfig()));
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();
    install(new ValidationModule(validatorFactory));
    install(AccessControlCoreModule.getInstance());
    install(DecisionModule.getInstance(config.getDecisionModuleConfiguration()));
    install(AggregatorModule.getInstance(config.getAggregatorConfiguration()));

    // TODO{phoenikx}  remove this later on
    install(AccessControlClientModule.getInstance(
        config.getAccessControlClientConfiguration(), ACCESS_CONTROL_SERVICE.getServiceId(), false));

    install(new ResourceGroupClientModule(config.getResourceGroupClientConfiguration().getResourceGroupServiceConfig(),
        config.getResourceGroupClientConfiguration().getResourceGroupServiceSecret(),
        ACCESS_CONTROL_SERVICE.getServiceId()));
    install(new UserClientModule(config.getUserClientConfiguration().getUserServiceConfig(),
        config.getUserClientConfiguration().getUserServiceSecret(), ACCESS_CONTROL_SERVICE.getServiceId()));

    MapBinder<String, ScopeLevel> scopesByKey = MapBinder.newMapBinder(binder(), String.class, ScopeLevel.class);
    scopesByKey.addBinding(ACCOUNT.toString()).toInstance(ACCOUNT);
    scopesByKey.addBinding(ORGANIZATION.toString()).toInstance(ORGANIZATION);
    scopesByKey.addBinding(PROJECT.toString()).toInstance(PROJECT);
    bind(ScopeParamsFactory.class).to(HarnessScopeParamsFactory.class);

    bind(HarnessResourceGroupService.class).to(HarnessResourceGroupServiceImpl.class);

    MapBinder<PrincipalType, PrincipalValidator> validatorByPrincipalType =
        MapBinder.newMapBinder(binder(), PrincipalType.class, PrincipalValidator.class);
    validatorByPrincipalType.addBinding(USER).to(UserValidator.class);

    Multibinder<EventConsumer> eventConsumers =
        Multibinder.newSetBinder(binder(), EventConsumer.class, Names.named(ENTITY_CRUD));
    eventConsumers.addBinding().to(ResourceGroupEventConsumer.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {}
}
