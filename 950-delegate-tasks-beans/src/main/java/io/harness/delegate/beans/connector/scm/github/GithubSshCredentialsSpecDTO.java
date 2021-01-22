package io.harness.delegate.beans.connector.scm.github;

import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GithubSshCredentialsSpec")
public class GithubSshCredentialsSpecDTO implements DecryptableEntity {
  @NotNull @SecretReference @ApiModelProperty(dataType = "string") SecretRefData sshKeyRef;
}
