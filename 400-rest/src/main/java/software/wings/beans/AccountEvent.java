package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@TargetModule(HarnessModule._957_CG_BEANS)
public class AccountEvent {
  private AccountEventType accountEventType;
  private String customMsg;
  private String category;
  private Map<String, String> properties;
}
