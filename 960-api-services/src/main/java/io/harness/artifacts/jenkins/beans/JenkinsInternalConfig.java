/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.artifacts.jenkins.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.jenkins.service.JenkinsRegistryUtils;
import io.harness.encryption.Encrypted;

import com.github.reinert.jjschema.Attributes;
import java.net.URI;
import javax.ws.rs.core.UriBuilder;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@ToString(exclude = "password")
@OwnedBy(HarnessTeam.CDC)
public class JenkinsInternalConfig {
  public static final String USERNAME_DEFAULT_TEXT = "UserName/Password";
  @Attributes(title = "Jenkins URL", required = true) @NotEmpty private String jenkinsUrl;
  boolean isCertValidationRequired;
  @Attributes(title = "Authentication Mechanism", required = true,
      enums = {USERNAME_DEFAULT_TEXT, JenkinsRegistryUtils.TOKEN_FIELD})
  @NotEmpty
  private String authMechanism;

  @Attributes(title = "Username") private String username;
  @Attributes(title = "Password/ API Token") @Encrypted(fieldName = "password/api_token") private char[] password;
  @Attributes(title = "Bearer Token(HTTP Header)") @Encrypted(fieldName = "bearer_token") private char[] token;

  public boolean hasCredentials() {
    return isNotEmpty(username);
  }

  public String getDockerRegistryUrl() {
    URI uri = UriBuilder.fromUri(jenkinsUrl).build();
    return UriBuilder.fromUri(jenkinsUrl).path(uri.getPath().endsWith("/") ? "" : "/").build().toString();
  }
}
