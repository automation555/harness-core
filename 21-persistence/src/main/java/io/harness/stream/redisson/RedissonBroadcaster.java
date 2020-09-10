/*
 * Copyright 2008-2020 Async-IO.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.harness.stream.redisson;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.stream.redisson.RedissonFactory.REDIS_ENV_NAMESPACE;

import io.harness.annotations.dev.OwnedBy;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.util.AbstractBroadcasterProxy;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple {@link org.atmosphere.cpr.Broadcaster} implementation based on Redisson
 *
 * @author Michael Gerlyand
 */
@OwnedBy(PL)
@NoArgsConstructor
@Slf4j
public class RedissonBroadcaster extends AbstractBroadcasterProxy {
  private static volatile RedissonClient redissonClient;
  private final AtomicBoolean isClosed = new AtomicBoolean();
  private RTopic topic;
  private Integer messageListenerRegistrationId;
  private static final String BROADCASTER_PREFIX = "hStreams";

  @Override
  public Broadcaster initialize(String id, AtmosphereConfig config) {
    return initialize(id, URI.create("http://localhost:6379"), config);
  }

  @Override
  public Broadcaster initialize(String id, URI uri, AtmosphereConfig config) {
    super.initialize(id, URI.create("http://localhost:6379"), config);
    setUp();
    return this;
  }

  private synchronized void setUp() {
    if (redissonClient == null) {
      redissonClient = RedissonFactory.getRedissonClient(config);
    }
    String broadcasterNamespace = isEmpty(config.getServletConfig().getInitParameter(REDIS_ENV_NAMESPACE))
        ? BROADCASTER_PREFIX
        : config.getServletConfig().getInitParameter(REDIS_ENV_NAMESPACE).concat(":").concat(BROADCASTER_PREFIX);
    String topicName = String.format("%s:%s", broadcasterNamespace, getID());
    topic = redissonClient.getTopic(topicName, redissonClient.getConfig().getCodec());
    config.shutdownHook(() -> {
      redissonClient.shutdown();
      isClosed.set(true);
    });
  }

  @Override
  public synchronized void setID(String id) {
    super.setID(id);
    setUp();
    reconfigure();
  }

  private synchronized void addMessageListener() {
    if (isNotEmpty(getAtmosphereResources()) && messageListenerRegistrationId == null && topic != null) {
      messageListenerRegistrationId =
          topic.addListener(Object.class, (channel, message) -> broadcastReceivedMessage(message));
      logger.info("Added message listener to topic");
    }
  }

  private synchronized void removeMessageListener() {
    if (isEmpty(getAtmosphereResources()) && messageListenerRegistrationId != null && topic != null) {
      topic.removeListener(messageListenerRegistrationId);
      messageListenerRegistrationId = null;
      logger.info("Removed message listener from topic");
    }
  }

  @Override
  public Broadcaster addAtmosphereResource(AtmosphereResource resource) {
    Broadcaster result = super.addAtmosphereResource(resource);
    addMessageListener();
    return result;
  }

  @Override
  public Broadcaster removeAtmosphereResource(AtmosphereResource resource) {
    Broadcaster result = super.removeAtmosphereResource(resource);
    removeMessageListener();
    return result;
  }

  @Override
  public synchronized void destroy() {
    if (!isClosed.get()) {
      topic.removeAllListeners();
      topic = null;
    }
    super.destroy();
  }

  @Override
  public void incomingBroadcast() {
    logger.info("Subscribing to: {}", getID());
  }

  @Override
  public void outgoingBroadcast(Object message) {
    topic.publish(message);
  }
}