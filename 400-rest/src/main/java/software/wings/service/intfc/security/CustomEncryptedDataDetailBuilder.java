package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EncryptedData;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;

@OwnedBy(PL)
@TargetModule(HarnessModule._890_SM_CORE)
public interface CustomEncryptedDataDetailBuilder {
  EncryptedDataDetail buildEncryptedDataDetail(
      EncryptedData encryptedData, CustomSecretsManagerConfig customSecretsManagerConfig);
}
