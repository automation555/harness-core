package software.wings.common.cache;

import static software.wings.beans.ResponseMessage.ResponseTypeEnum.ERROR;

import org.apache.commons.lang3.text.StrSubstitutor;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.exception.WingsException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * The Class ResponseCodeCache.
 */
public class ResponseCodeCache {
  private static final String RESPONSE_MESSAGE_FILE = "/response_messages.properties";

  private static final ResponseCodeCache instance = new ResponseCodeCache();

  private final Properties messages;

  private ResponseCodeCache() {
    messages = new Properties();
    try (InputStream in = getClass().getResourceAsStream(RESPONSE_MESSAGE_FILE)) {
      messages.load(in);
    } catch (IOException exception) {
      throw new WingsException(exception);
    }
  }

  /**
   * Gets instance.
   *
   * @return singleton instance for the cache.
   */
  public static ResponseCodeCache getInstance() {
    return instance;
  }

  /**
   * Converts error code into ResponseMessage object.
   *
   * @param errorCode errorCode for which message is needed.
   * @return ResponseMessage Object.
   */
  public ResponseMessage getResponseMessage(ErrorCode errorCode) {
    String message = messages.getProperty(errorCode.getCode());
    if (message == null) {
      return null;
    }
    return ResponseMessage.builder().code(errorCode).message(message).errorType(ERROR).build();
  }

  /**
   * Converts error code and map of key value pairs for substitution into ResponseMessage object .
   *
   * @param errorCode errorCode for which message is needed.
   * @param params    for substituting in ResponseMessage
   * @return ResponseMessage object.
   */
  public ResponseMessage getResponseMessage(ErrorCode errorCode, Map<String, Object> params) {
    String message = getMessage(errorCode, params);
    return ResponseMessage.builder().code(errorCode).message(message).errorType(ERROR).build();
  }

  /**
   * Converts error code and map of key value pairs for substitution into ResponseMessage object .
   * @param errorCode
   * @param responseTypeEnum
   * @param params
   * @return
   */
  public ResponseMessage getResponseMessage(
      ErrorCode errorCode, ResponseTypeEnum responseTypeEnum, Map<String, Object> params) {
    String message = getMessage(errorCode, params);
    return ResponseMessage.builder().code(errorCode).message(message).errorType(ERROR).build();
  }

  private String getMessage(ErrorCode errorCode, Map<String, Object> params) {
    String message = messages.getProperty(errorCode.getCode());
    if (message == null) {
      message = errorCode.name();
    }
    message = StrSubstitutor.replace(message, params);
    return message;
  }
}
