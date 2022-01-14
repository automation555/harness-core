package io.harness.exception;

import static io.harness.eraro.ErrorCode.INSTANCE_STATS_MIGRATION_ERROR;

import io.harness.eraro.Level;

public class InstanceMigrationException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public InstanceMigrationException(String message, Throwable cause) {
    super(message, cause, INSTANCE_STATS_MIGRATION_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}