package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;
import software.wings.beans.VaultConfig;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionConfig;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class SecretManagerDecryptValidation extends AbstractSecretManagerValidation {
  public SecretManagerDecryptValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof EncryptedDataDetail)
                             .map(obj -> {
                               EncryptedDataDetail encryptedDataDetail = (EncryptedDataDetail) obj;
                               String secretManagerUrl = "https://aws.amazon.com/";
                               EncryptionConfig encryptionConfig = encryptedDataDetail.getEncryptionConfig();
                               if (encryptedDataDetail.getEncryptionType().equals(EncryptionType.VAULT)) {
                                 secretManagerUrl =
                                     SettingVariableTypes.VAULT + ":" + ((VaultConfig) encryptionConfig).getVaultUrl();
                               }
                               return secretManagerUrl;
                             })
                             .findFirst()
                             .orElse(null));
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    for (Object parmeter : getParameters()) {
      if (parmeter instanceof EncryptedDataDetail) {
        return ((EncryptedDataDetail) parmeter).getEncryptionConfig();
      }
    }
    throw new IllegalStateException(
        "No encryption details were passed to config {} " + Arrays.toString(getParameters()));
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    return singletonList(super.validateSecretManager());
  }
}
