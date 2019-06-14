package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.beans.DelegateTask;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class AcrValidation extends AbstractDelegateValidateTask {
  private static final String ACR_URL = "https://azure.microsoft.com/";

  public AcrValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(ACR_URL);
  }
}