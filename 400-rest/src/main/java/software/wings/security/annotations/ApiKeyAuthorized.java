package software.wings.security.annotations;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@OwnedBy(CDC)
public @interface ApiKeyAuthorized {
  PermissionType permissionType() default PermissionType.NONE;

  Action action() default Action.DEFAULT;

  boolean allowEmptyApiKey() default false;

  boolean skipAuth() default false;
}
