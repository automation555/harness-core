package io.harness.event.client.impl.tailer;

import static com.google.common.base.Preconditions.checkArgument;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

final class Sampler {
  private final Duration interval;
  private final Clock clock;

  private Instant lastSampledAt;
  private boolean shouldSample;

  Sampler(Duration interval, Clock clock) {
    checkArgument(!interval.isNegative(), "Sampling interval should be non-negative");
    this.interval = interval;
    this.clock = clock;
    this.lastSampledAt = Instant.EPOCH;
    updateTime();
  }

  Sampler(Duration interval) {
    this(interval, Clock.systemUTC());
  }

  // Don't merge the 2 methods below. We want to use the currentTime across multiple calls to sampled.
  void updateTime() {
    Instant currentTime = Instant.now(clock);
    shouldSample = Duration.between(lastSampledAt, currentTime).compareTo(interval) >= 0;
    if (shouldSample) {
      lastSampledAt = currentTime;
    }
  }

  void sampled(Runnable runnable) {
    if (shouldSample) {
      runnable.run();
    }
  }
}
