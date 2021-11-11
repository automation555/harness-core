package io.harness.cvng.core.services.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.cvng.beans.DatadogLogDataCollectionInfo;
import io.harness.cvng.beans.datadog.*;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.beans.TimeSeriesSampleDTO;
import io.harness.cvng.core.beans.datadog.DatadogDashboardDTO;
import io.harness.cvng.core.beans.datadog.DatadogDashboardDetail;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.services.api.DatadogService;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.exception.OnboardingException;
import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.serializer.JsonUtils;
import io.harness.utils.PageUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

@Slf4j
public class DatadogServiceImpl implements DatadogService {

    @Inject
    private OnboardingService onboardingService;

    @Override
    public PageResponse<DatadogDashboardDTO> getAllDashboards(ProjectParams projectParams, String connectorIdentifier,
                                                              int pageSize, int offset, String filter, String tracingId) {
        DataCollectionRequest<DatadogConnectorDTO> request =
                DatadogDashboardListRequest.builder().type(DataCollectionRequestType.DATADOG_DASHBOARD_LIST).build();

        Type type = new TypeToken<List<DatadogDashboardDTO>>() {
        }.getType();
        List<DatadogDashboardDTO> dashboardList = performRequestAndGetDataResult(request,
                type,
                projectParams,
                connectorIdentifier,
                tracingId);

        List<DatadogDashboardDTO> filteredList = dashboardList;
        if (isNotEmpty(filter)) {
            filteredList = dashboardList.stream()
                    .filter(dashboardDto -> dashboardDto.getName() != null
                            && dashboardDto.getName().toLowerCase().contains(filter.toLowerCase()))
                    .collect(Collectors.toList());
        }
        return PageUtils.offsetAndLimit(filteredList, offset, pageSize);
    }

    @Override
    public List<DatadogDashboardDetail> getDashboardDetails(ProjectParams projectParams,
                                                            String connectorIdentifier,
                                                            String dashboardId,
                                                            String tracingId) {
        DataCollectionRequest<DatadogConnectorDTO> request =
                DatadogDashboardDetailsRequest.builder().dashboardId(dashboardId).type(DataCollectionRequestType.DATADOG_DASHBOARD_DETAILS).build();

        Type type = new TypeToken<List<DatadogDashboardDetail>>() {
        }.getType();
        return performRequestAndGetDataResult(request,
                type,
                projectParams,
                connectorIdentifier,
                tracingId);
    }

    @Override
    public List<String> getMetricTagsList(ProjectParams projectParams, String connectorIdentifier, String metricName, String tracingId) {
        DataCollectionRequest<DatadogConnectorDTO> request =
                DatadogMetricTagsRequest.builder().metric(metricName).type(DataCollectionRequestType.DATADOG_METRIC_TAGS)
                        .build();

        Type type = new TypeToken<List<String>>() {
        }.getType();
        return performRequestAndGetDataResult(
                request,
                type,
                projectParams,
                connectorIdentifier,
                tracingId);
    }

    @Override
    public List<String> getActiveMetrics(ProjectParams projectParams, String connectorIdentifier, String tracingId) {
        DataCollectionRequest<DatadogConnectorDTO> request =
                DatadogActiveMetricsRequest.builder().from(0).type(DataCollectionRequestType.DATADOG_ACTIVE_METRICS)
                        .build();

        Type type = new TypeToken<List<String>>() {
        }.getType();
        return performRequestAndGetDataResult(request,
                type,
                projectParams,
                connectorIdentifier,
                tracingId);
    }

    @Override
    public List<TimeSeriesSampleDTO> getTimeSeriesPoints(ProjectParams projectParams,
                                                         String connectorIdentifier,
                                                         String query,
                                                         String tracingId) {
        Instant now = DateTimeUtils.roundDownTo1MinBoundary(Instant.now());

        DataCollectionRequest<DatadogConnectorDTO> request =
                DatadogTimeSeriesPointsRequest.builder()
                        .type(DataCollectionRequestType.DATADOG_TIME_SERIES_POINTS)
                        .from(now.minus(Duration.ofMinutes(60)).getEpochSecond())
                        .to(now.getEpochSecond())
                        .query(query)
                        .build();
        Type type = new TypeToken<List<TimeSeriesSampleDTO>>() {
        }.getType();

        return performRequestAndGetDataResult(request,
                type,
                projectParams,
                connectorIdentifier,
                tracingId);
    }

    @Override
    public List<LinkedHashMap> getSampleLogData(ProjectParams projectParams, String connectorIdentifier, String query, String tracingId) {
        try {
            Instant now = DateTimeUtils.roundDownTo1MinBoundary(Instant.now());

            DataCollectionRequest<DatadogConnectorDTO> request = DatadogLogSampleDataRequest.builder()
                    .type(DataCollectionRequestType.DATADOG_LOG_SAMPLE_DATA)
                    .from(now.minus(Duration.ofMinutes(1000)).toEpochMilli())
                    .to(now.toEpochMilli())
                    .limit(DatadogLogDataCollectionInfo.LOG_MAX_LIMIT)
                    .build();

            Type type = new TypeToken<List<LinkedHashMap>>() {
            }.getType();
            return performRequestAndGetDataResult(request, type, projectParams, connectorIdentifier, tracingId);
        } catch (Exception ex) {
            String msg = "Exception while trying to fetch sample data. Please ensure that the query is valid.";
            log.error(msg, ex);
            throw new OnboardingException(msg);
        }
    }

    @Override
    public List<String> getLogIndexes(ProjectParams projectParams,
                                      String connectorIdentifier,
                                      String tracingId) {
        DataCollectionRequest<DatadogConnectorDTO> request =
                DatadogLogIndexesRequest.builder()
                        .type(DataCollectionRequestType.DATADOG_LOG_INDEXES)
                        .build();

        Type type = new TypeToken<List<String>>() {
        }.getType();
        return performRequestAndGetDataResult(request,
                type,
                projectParams,
                connectorIdentifier,
                tracingId);
    }

    private <T> T performRequestAndGetDataResult(DataCollectionRequest<DatadogConnectorDTO> dataCollectionRequest,
                                                 Type type,
                                                 ProjectParams projectParams,
                                                 String connectorIdentifier,
                                                 String tracingId) {
        OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                .dataCollectionRequest(dataCollectionRequest)
                .connectorIdentifier(connectorIdentifier)
                .accountId(projectParams.getAccountIdentifier())
                .orgIdentifier(projectParams.getOrgIdentifier())
                .projectIdentifier(projectParams.getProjectIdentifier())
                .tracingId(tracingId)
                .build();

        OnboardingResponseDTO response = onboardingService.getOnboardingResponse(projectParams.getAccountIdentifier(), onboardingRequestDTO);
        return new Gson().fromJson(JsonUtils.asJson(response.getResult()), type);
    }
}
