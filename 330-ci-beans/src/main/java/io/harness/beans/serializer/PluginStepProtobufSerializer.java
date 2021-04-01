package io.harness.beans.serializer;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.callback.DelegateCallbackToken;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.product.ci.engine.proto.PluginStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.core.timeout.TimeoutUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class PluginStepProtobufSerializer implements ProtobufStepSerializer<PluginStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Override

  public UnitStep serializeStep(StepElementConfig step, Integer port, String callbackId, String logKey) {
    CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
    PluginStepInfo pluginStepInfo = (PluginStepInfo) ciStepInfo;

    long timeout = TimeoutUtils.getTimeoutInSeconds(step.getTimeout(), ciStepInfo.getDefaultTimeout());
    StepContext stepContext =
        StepContext.newBuilder().setNumRetries(pluginStepInfo.getRetry()).setExecutionTimeoutSecs(timeout).build();
    if (port == null) {
      throw new CIStageExecutionException("Port can not be null");
    }

    if (callbackId == null) {
      throw new CIStageExecutionException("CallbackId can not be null");
    }
    PluginStep pluginStep = PluginStep.newBuilder()
                                .setContainerPort(port)
                                .setImage(RunTimeInputHandler.resolveStringParameter(
                                    "Image", "Plugin", step.getIdentifier(), pluginStepInfo.getImage(), true))
                                .setContext(stepContext)
                                .build();

    String skipCondition = SkipConditionUtils.getSkipCondition(step);
    return UnitStep.newBuilder()
        .setId(step.getIdentifier())
        .setTaskId(callbackId)
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(step.getName()).orElse(""))
        .setSkipCondition(Optional.ofNullable(skipCondition).orElse(""))
        .setPlugin(pluginStep)
        .setLogKey(logKey)
        .build();
  }

  public UnitStep serializeStepWithStepParameters(PluginStepInfo pluginStepInfo, Integer port, String callbackId,
      String logKey, String identifier, ParameterField<Timeout> parameterFieldTimeout, String accountId,
      String stepName) {
    if (callbackId == null) {
      throw new CIStageExecutionException("CallbackId can not be null");
    }

    if (port == null) {
      throw new CIStageExecutionException("Port can not be null");
    }

    long timeout = TimeoutUtils.getTimeoutInSeconds(parameterFieldTimeout, pluginStepInfo.getDefaultTimeout());
    StepContext stepContext = StepContext.newBuilder().setExecutionTimeoutSecs(timeout).build();

    PluginStep pluginStep = PluginStep.newBuilder()
                                .setContainerPort(port)
                                .setImage(RunTimeInputHandler.resolveStringParameter(
                                    "Image", "Plugin", identifier, pluginStepInfo.getImage(), true))
                                .setContext(stepContext)
                                .build();

    return UnitStep.newBuilder()
        .setId(identifier)
        .setTaskId(callbackId)
        .setAccountId(accountId)
        .setContainerPort(port)
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(stepName)
        .setPlugin(pluginStep)
        .setLogKey(logKey)
        .build();
  }
}
