package io.harness.serializer.spring;

import io.harness.advisers.fail.OnFailAdviserParameters;
import io.harness.advisers.ignore.IgnoreAdviserParameters;
import io.harness.advisers.retry.RetryAdviserParameters;
import io.harness.advisers.success.OnSuccessAdviserParameters;
import io.harness.spring.AliasRegistrar;
import io.harness.state.core.dummy.DummySectionOutcome;
import io.harness.state.core.dummy.DummySectionStepParameters;
import io.harness.state.core.dummy.DummySectionStepTransput;
import io.harness.state.core.dummy.DummyStepParameters;
import io.harness.state.core.fork.ForkStepParameters;
import io.harness.state.core.section.SectionStepParameters;
import io.harness.state.core.section.chain.SectionChainPassThroughData;
import io.harness.state.core.section.chain.SectionChainStepParameters;

import java.util.Map;

/**
 * DO NOT CHANGE the keys. This is how track the Interface Implementations
 */
public class OrchestrationAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("dummySectionOutcome", DummySectionOutcome.class);
    orchestrationElements.put("dummySectionStepParameters", DummySectionStepParameters.class);
    orchestrationElements.put("dummySectionStepTransput", DummySectionStepTransput.class);
    orchestrationElements.put("dummyStepParameters", DummyStepParameters.class);
    orchestrationElements.put("forkStepParameters", ForkStepParameters.class);
    orchestrationElements.put("ignoreAdviserParameters", IgnoreAdviserParameters.class);
    orchestrationElements.put("onFailAdviserParameters", OnFailAdviserParameters.class);
    orchestrationElements.put("onSuccessAdviserParameters", OnSuccessAdviserParameters.class);
    orchestrationElements.put("retryAdviserParameters", RetryAdviserParameters.class);
    orchestrationElements.put("sectionChainPassThroughData", SectionChainPassThroughData.class);
    orchestrationElements.put("sectionChainStepParameters", SectionChainStepParameters.class);
    orchestrationElements.put("sectionStepParameters", SectionStepParameters.class);
  }
}
