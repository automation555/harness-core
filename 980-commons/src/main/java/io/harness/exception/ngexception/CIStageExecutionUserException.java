package io.harness.exception.ngexception;

import static io.harness.eraro.ErrorCode.NG_PIPELINE_EXECUTION_EXCEPTION;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class CIStageExecutionUserException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public CIStageExecutionUserException(String message) {
    super(message, null, NG_PIPELINE_EXECUTION_EXCEPTION, Level.ERROR, null, null);
    param(MESSAGE_KEY, message);
  }

  public CIStageExecutionUserException(String message, Throwable cause) {
    super(message, cause, NG_PIPELINE_EXECUTION_EXCEPTION, Level.ERROR, null, null);
  }
}
