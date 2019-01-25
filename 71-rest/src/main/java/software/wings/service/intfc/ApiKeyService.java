package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.ApiKeyEntry;
import software.wings.service.intfc.ownership.OwnedByAccount;

public interface ApiKeyService extends OwnedByAccount {
  PageResponse<ApiKeyEntry> list(PageRequest<ApiKeyEntry> request);
  String generate(@NotEmpty String accountId);
  void delete(@NotEmpty String uuid);
  String get(@NotEmpty String uuid, @NotEmpty String accountId);
  void validate(@NotEmpty String key, @NotEmpty String accountId);
}
