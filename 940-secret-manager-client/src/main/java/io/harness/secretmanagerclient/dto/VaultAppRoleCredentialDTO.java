package io.harness.secretmanagerclient.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.SecretRefData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(PL)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("APP_ROLE")
public class VaultAppRoleCredentialDTO extends VaultCredentialDTO {
  private String appRoleId;
  @ApiModelProperty(dataType = "string") private SecretRefData secretId;
}
