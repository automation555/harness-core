package io.harness.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ServiceNowException;
import io.harness.exception.WingsException;

import java.net.MalformedURLException;
import java.net.URL;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@OwnedBy(CDC)
@UtilityClass
public class ServiceNowUtils {
  public String prepareTicketUrlFromTicketNumber(String baseUrl, String ticketNumber, String ticketType) {
    try {
      URL issueUrl = new URL(getUrlWithPath(baseUrl, ticketType) + ".do?sysparm_query=number=" + ticketNumber);
      return issueUrl.toString();
    } catch (MalformedURLException e) {
      throw new ServiceNowException(
          String.format("Invalid serviceNow base url: %s", baseUrl), ErrorCode.SERVICENOW_ERROR, WingsException.USER);
    }
  }

  public String prepareTicketUrlFromTicketId(String baseUrl, String ticketId, String ticketType) {
    return getUrlWithPath(baseUrl, ticketType) + ".do?sys_id=" + ticketId;
  }

  @NotNull
  private static String getUrlWithPath(String baseUrl, String ticketType) {
    return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "nav_to.do?uri=/" + ticketType.toLowerCase();
  }
}
