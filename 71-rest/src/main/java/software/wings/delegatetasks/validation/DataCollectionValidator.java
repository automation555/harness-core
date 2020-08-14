package software.wings.delegatetasks.validation;

import com.google.common.collect.Lists;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.security.encryption.EncryptionConfig;
import software.wings.service.impl.analysis.DataCollectionInfoV2;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class DataCollectionValidator extends AbstractSecretManagerValidation {
  public DataCollectionValidator(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    DataCollectionInfoV2 dataCollectionInfoV2 = (DataCollectionInfoV2) getParameters()[0];
    Optional<String> criteria = dataCollectionInfoV2.getUrlForValidation();
    return criteria.map(Lists::newArrayList).orElse(Lists.newArrayList());
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    DataCollectionInfoV2 dataCollectionInfoV2 = (DataCollectionInfoV2) getParameters()[0];
    return dataCollectionInfoV2.getEncryptionConfig().orElse(super.getEncryptionConfig());
  }
}
