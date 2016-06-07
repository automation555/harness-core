package software.wings.beans;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import javax.ws.rs.core.Response.Status;

/**
 * The type Error constants.
 */
public class ErrorConstants {
  /**
   * The constant DEFAULT_ERROR_CODE.
   */
  public static final String DEFAULT_ERROR_CODE = "DEFAULT_ERROR_CODE";
  /**
   * The constant INVALID_ARGUMENT.
   */
  public static final String INVALID_ARGUMENT = "INVALID_ARGUMENT";
  /**
   * The constant INVALID_TOKEN.
   */
  public static final String INVALID_TOKEN = "INVALID_TOKEN";
  /**
   * The constant EXPIRED_TOKEN.
   */
  public static final String EXPIRED_TOKEN = "EXPIRED_TOKEN";
  /**
   * The constant ACCESS_DENIED.
   */
  public static final String ACCESS_DENIED = "ACCESS_DENIED";

  /**
   * The constant INVALID_CREDENTIAL.
   */
  public static final String INVALID_CREDENTIAL = "INVALID_CREDENTIAL_ERROR";
  /**
   * The constant INVALID_KEY.
   */
  public static final String INVALID_KEY = "INVALID_KEY_ERROR";
  /**
   * The constant INVALID_KEYPATH.
   */
  public static final String INVALID_KEYPATH = "INVALID_KEYPATH_ERROR";
  /**
   * The constant UNKNOWN_HOST.
   */
  public static final String UNKNOWN_HOST = "UNKNOWN_HOST_ERROR";
  /**
   * The constant UNREACHABLE_HOST.
   */
  public static final String UNREACHABLE_HOST = "UNREACHABLE_HOST_ERROR";
  /**
   * The constant INVALID_PORT.
   */
  public static final String INVALID_PORT = "INVALID_OR_BLOCKED_PORT_ERROR";
  /**
   * The constant SSH_SESSION_TIMEOUT.
   */
  public static final String SSH_SESSION_TIMEOUT = "SSH_SESSION_TIMEOUT";
  /**
   * The constant SOCKET_CONNECTION_ERROR.
   */
  public static final String SOCKET_CONNECTION_ERROR = "SSH_SOCKET_CONNECTION_ERROR";
  /**
   * The constant SOCKET_CONNECTION_TIMEOUT.
   */
  public static final String SOCKET_CONNECTION_TIMEOUT = "SOCKET_CONNECTION_TIMEOUT_ERROR";
  /**
   * The constant UNKNOWN_ERROR.
   */
  public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";
  /**
   * The constant UNKNOWN_EXECUTOR_TYPE_ERROR.
   */
  public static final String UNKNOWN_EXECUTOR_TYPE_ERROR = "UNKNOWN_EXECUTOR_TYPE_ERROR";

  /**
   * The constant DUPLICATE_STATE_NAMES.
   */
  public static final String DUPLICATE_STATE_NAMES = "DUPLICATE_STATE_NAMES";
  /**
   * The constant TRANSITION_NOT_LINKED.
   */
  public static final String TRANSITION_NOT_LINKED = "TRANSITION_NOT_LINKED";
  /**
   * The constant TRANSITION_TO_INCORRECT_STATE.
   */
  public static final String TRANSITION_TO_INCORRECT_STATE = "TRANSITION_TO_INCORRECT_STATE";
  /**
   * The constant TRANSITION_TYPE_NULL.
   */
  public static final String TRANSITION_TYPE_NULL = "TRANSITION_TYPE_NULL";
  /**
   * The constant STATES_WITH_DUP_TRANSITIONS.
   */
  public static final String STATES_WITH_DUP_TRANSITIONS = "STATES_WITH_DUP_TRANSITIONS";
  /**
   * The constant NON_FORK_STATES.
   */
  public static final String NON_FORK_STATES = "NON_FORK_STATES";
  /**
   * The constant NON_REPEAT_STATES.
   */
  public static final String NON_REPEAT_STATES = "NON_REPEAT_STATES";
  /**
   * The constant INITIAL_STATE_NOT_DEFINED.
   */
  public static final String INITIAL_STATE_NOT_DEFINED = "INITIAL_STATE_NOT_DEFINED";
  /**
   * The constant FILE_INTEGRITY_CHECK_FAILED.
   */
  public static final String FILE_INTEGRITY_CHECK_FAILED = "FILE_INTEGRITY_CHECK_FAILED";
  /**
   * The constant INVALID_URL.
   */
  public static final String INVALID_URL = "INVALID_URL";
  /**
   * The constant FILE_DOWNLOAD_FAILED.
   */
  public static final String FILE_DOWNLOAD_FAILED = "FILE_DOWNLOAD_FAILED";
  /**
   * The constant PLATFORM_SOFTWARE_DELETE_ERROR.
   */
  public static final String PLATFORM_SOFTWARE_DELETE_ERROR = "PLATFORM_SOFTWARE_DELETE_ERROR";
  /**
   * The constant INVALID_CSV_FILE.
   */
  public static final String INVALID_CSV_FILE = "INVALID_CSV_FILE";
  /**
   * The constant ARGS_NAME.
   */
  public static final String ARGS_NAME = "ARGS_NAME";
  /**
   * The constant INVALID_REQUEST.
   */
  public static final String INVALID_REQUEST = "INVALID_REQUEST";
  /**
   * The constant PIPELINE_ALREADY_TRIGGERED.
   */
  public static final String PIPELINE_ALREADY_TRIGGERED = "PIPELINE_ALREADY_TRIGGERED";
  /**
   * The constant NON_EXISTING_PIPELINE.
   */
  public static final String NON_EXISTING_PIPELINE = "NON_EXISTING_PIPELINE";

  /**
   * The constant DUPLICATE_COMMAND_NAMES.
   */
  public static final String DUPLICATE_COMMAND_NAMES = "DUPLICATE_COMMAND_NAMES";
  /**
   * The constant INVALID_PIPELINE.
   */
  public static final String INVALID_PIPELINE = "INVALID_PIPELINE";

  /**
   * Http error code mapper status.
   *
   * @param errorCode the error code
   * @return the status
   */
  public static Status httpErrorCodeMapper(String errorCode) {
    switch (errorCode == null ? "" : errorCode) {
      case INVALID_TOKEN:
      case EXPIRED_TOKEN:
      case INVALID_CREDENTIAL:
        return UNAUTHORIZED;
      case ACCESS_DENIED:
        return FORBIDDEN;
      default:
        return BAD_REQUEST;
    }
  }
}
