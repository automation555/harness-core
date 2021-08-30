package io.harness.pms.async.plan;

import static io.harness.eventsframework.EventsFrameworkConstants.PLAN_NOTIFY_EVENT_TOPIC;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;
import io.harness.queue.QueueController;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class PlanNotifyEventConsumer extends PmsAbstractRedisConsumer<PlanNotifyMessageListener> {
  public static final String PMS_PLAN_CREATION = "pms_plan_creation";

  @Inject
  public PlanNotifyEventConsumer(@Named(PLAN_NOTIFY_EVENT_TOPIC) Consumer redisConsumer,
      PlanNotifyMessageListener messageListener, @Named("sdkEventsCache") Cache<String, Integer> eventsCache,
      QueueController queueController) {
    super(redisConsumer, messageListener, eventsCache, queueController);
  }
}
