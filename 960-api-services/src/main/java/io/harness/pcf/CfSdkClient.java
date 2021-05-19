package io.harness.pcf;

import io.harness.logging.LogCallback;
import io.harness.pcf.model.CfRequestConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.routes.Route;

public interface CfSdkClient {
  /**
   * Get organizations.
   *
   * @param pcfRequestConfig PcfRequestConfig
   * @return List of OrganizationSummary
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  List<OrganizationSummary> getOrganizations(CfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Get space for organization.
   *
   * @param pcfRequestConfig PcfRequestConfig
   * @return List of String
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  List<String> getSpacesForOrganization(CfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Get applications.
   *
   * @param pcfRequestConfig PcfRequestConfig
   * @return List of ApplicationSummary
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  List<ApplicationSummary> getApplications(CfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Get application by name.
   *
   * @param pcfRequestConfig PcfRequestConfig
   * @return ApplicationDetail
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  ApplicationDetail getApplicationByName(CfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Start applications.
   *
   * @param pcfRequestConfig
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void startApplication(CfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;

  /**
   * Scale application.
   *
   * @param pcfRequestConfig
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void scaleApplications(CfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;

  /**
   * Stop application.
   *
   * @param pcfRequestConfig
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void stopApplication(CfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;

  /**
   * Delete application.
   *
   * @param pcfRequestConfig
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void deleteApplication(CfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;

  /**
   * Push application.
   *
   * @param pcfRequestConfig
   * @param path Path
   * @param executionLogCallback
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void pushAppBySdk(CfRequestConfig pcfRequestConfig, Path path, LogCallback executionLogCallback)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Create route.
   *
   * @param pcfRequestConfig
   * @param host
   * @param domain
   * @param path
   * @param tcpRoute
   * @param useRandomPort
   * @param port
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void createRouteMap(CfRequestConfig pcfRequestConfig, String host, String domain, String path, boolean tcpRoute,
      boolean useRandomPort, Integer port) throws PivotalClientApiException, InterruptedException;

  /**
   * Unmap route.
   *
   * @param pcfRequestConfig
   * @param route
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void unmapRouteMapForApp(CfRequestConfig pcfRequestConfig, Route route)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Map routes.
   *
   * @param pcfRequestConfig
   * @param routes
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void mapRoutesForApplication(CfRequestConfig pcfRequestConfig, List<String> routes)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Map route.
   *
   * @param pcfRequestConfig
   * @param route
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void mapRouteMapForApp(CfRequestConfig pcfRequestConfig, Route route)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Unmap route.
   *
   * @param pcfRequestConfig
   * @param routes
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void unmapRoutesForApplication(CfRequestConfig pcfRequestConfig, List<String> routes)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Get route.
   *
   * @param pcfRequestConfig
   * @param route
   * @return Optional of Route
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  Optional<Route> getRouteMap(CfRequestConfig pcfRequestConfig, String route)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Get route.
   *
   * @param paths
   * @param pcfRequestConfig
   * @return List of Route
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  List<Route> getRouteMapsByNames(List<String> paths, CfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Get routes for space.
   *
   * @param pcfRequestConfig
   * @return List of String
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  List<String> getRoutesForSpace(CfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  /**
   *
   *
   * @param pcfRequestConfig
   * @param logsAfterTsNs
   * @return List of LogMessage
   * @throws PivotalClientApiException
   */
  List<LogMessage> getRecentLogs(CfRequestConfig pcfRequestConfig, long logsAfterTsNs) throws PivotalClientApiException;

  /**
   * Get application environments by name.
   *
   * @param pcfRequestConfig
   * @return ApplicationEnvironments
   * @throws PivotalClientApiException
   */
  ApplicationEnvironments getApplicationEnvironmentsByName(CfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException;

  /**
   * Get tasks.
   *
   * @param pcfRequestConfig
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void getTasks(CfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;

  /**
   * Get all space domains.
   *
   * @param pcfRequestConfig
   * @return List of Domain
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  List<Domain> getAllDomainsForSpace(CfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;
}
