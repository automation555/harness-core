package software.wings.app;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isBlank;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.ErrorCode.INVALID_TOKEN;
import static software.wings.beans.ErrorCode.UNKNOWN_ERROR;
import static software.wings.beans.ResponseMessage.Level.ERROR;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.utils.Switch.unhandled;

import com.google.common.base.Splitter;
import com.google.inject.Inject;

import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.handler.AtmosphereHandlerAdapter;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AuthToken;
import software.wings.beans.Base;
import software.wings.beans.ErrorCode;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.AuthService;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.util.List;

/**
 * Created by peeyushaggarwal on 8/15/16.
 */
@AtmosphereHandlerService(path = "/stream/ui/{channel}", interceptors = {AtmosphereResourceLifecycleInterceptor.class},
    broadcasterCache = UUIDBroadcasterCache.class)
public class UiStreamHandler extends AtmosphereHandlerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(UiStreamHandler.class);

  public static final Splitter SPLITTER = Splitter.on("/").omitEmptyStrings();
  @Inject private AuthService authService;

  @Override
  public void onRequest(AtmosphereResource resource) throws IOException {
    AtmosphereRequest req = resource.getRequest();
    logger.info("Received request with  Broadcaster Id {}", resource.getBroadcaster().getID());
    if (req.getMethod().equals("GET")) {
      if (isBlank(req.getParameter("token"))) {
        sendError(resource, INVALID_TOKEN);
        return;
      }

      try {
        logger.info("Verifying Delegate token");
        AuthToken authToken = authService.validateToken(req.getParameter("token"));
        logger.info("Delegate token verified");

        List<String> pathSegments = SPLITTER.splitToList(req.getPathInfo());
        if (pathSegments.size() <= 5) {
          sendError(resource, INVALID_REQUEST);
        }

        String accountId = pathSegments.get(1);
        String appId = pathSegments.get(2).replace("all", Base.GLOBAL_APP_ID);
        String envId = pathSegments.get(3).replace("all", Base.GLOBAL_ENV_ID);
        Channel channel = Channel.getChannelByChannelName(pathSegments.get(5));

        if (channel == null) {
          sendError(resource, INVALID_REQUEST);
        }
        PermissionAttribute permissionAttribute =
            new PermissionAttribute(channel.getPermission(), channel.getScope(), "GET");
        logger.info("Verifying authorization");
        authService.authorize(accountId, appId, envId, authToken.getUser(), asList(permissionAttribute), null);
        logger.info("Authorization successful");

      } catch (WingsException e) {
        sendError(resource, e.getResponseMessageList().get(0).getCode());
        return;
      } catch (Exception e) {
        sendError(resource, UNKNOWN_ERROR);
        return;
      }

      resource.suspend();
    } else if (req.getMethod().equalsIgnoreCase("POST")) {
      resource.getBroadcaster().broadcast(req.getReader().readLine().trim());
    }
  }

  @Override
  public void onStateChange(AtmosphereResourceEvent event) throws IOException {
    AtmosphereResource r = event.getResource();
    AtmosphereResponse res = r.getResponse();

    if (r.isSuspended()) {
      Object message = event.getMessage() == null ? null : event.getMessage();
      if (message != null) {
        event.getResource().write(JsonUtils.asJson(message));
      }

      AtmosphereResource.TRANSPORT transport = r.transport();
      switch (transport) {
        case JSONP:
        case LONG_POLLING:
          event.getResource().resume();
          break;
        case WEBSOCKET:
          break;
        case STREAMING:
          res.getWriter().flush();
          break;
        default:
          unhandled(transport);
      }
    }
  }

  private void sendError(AtmosphereResource resource, ErrorCode errorCode) throws IOException {
    AtmosphereResource.TRANSPORT transport = resource.transport();
    switch (transport) {
      case JSONP:
      case LONG_POLLING:
        resource.getResponse().sendError(errorCode.getStatus().getStatusCode());
        break;
      case WEBSOCKET:
        break;
      case STREAMING:
        break;
      default:
        unhandled(transport);
    }
    resource.write(JsonUtils.asJson(aResponseMessage()
                                        .code(errorCode)
                                        .level(ERROR)
                                        .message(ResponseCodeCache.getInstance().prepareMessage(errorCode, null))
                                        .build()));
    resource.close();
  }
}
