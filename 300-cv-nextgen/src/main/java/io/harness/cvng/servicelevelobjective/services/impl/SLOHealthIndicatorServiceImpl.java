package io.harness.cvng.servicelevelobjective.services.impl;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.servicelevelobjective.beans.ErrorBudgetRisk;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardWidget.SLOGraphData;
import io.harness.cvng.servicelevelobjective.entities.SLOHealthIndicator;
import io.harness.cvng.servicelevelobjective.entities.SLOHealthIndicator.SLOHealthIndicatorKeys;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelObjective;
import io.harness.cvng.servicelevelobjective.services.api.SLIRecordService;
import io.harness.cvng.servicelevelobjective.services.api.SLOHealthIndicatorService;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelIndicatorService;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelObjectiveService;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.mongodb.morphia.query.UpdateOperations;

public class SLOHealthIndicatorServiceImpl implements SLOHealthIndicatorService {
  @Inject private HPersistence hPersistence;
  @Inject private ServiceLevelObjectiveService serviceLevelObjectiveService;
  @Inject private ServiceLevelIndicatorService serviceLevelIndicatorService;
  @Inject private SLIRecordService sliRecordService;
  @Inject private Clock clock;

  @Override
  public List<SLOHealthIndicator> getByMonitoredServiceIdentifiers(
      ProjectParams projectParams, List<String> monitoredServiceIdentifiers) {
    return hPersistence.createQuery(SLOHealthIndicator.class)
        .filter(SLOHealthIndicatorKeys.accountId, projectParams.getAccountIdentifier())
        .filter(SLOHealthIndicatorKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(SLOHealthIndicatorKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .field(SLOHealthIndicatorKeys.monitoredServiceIdentifier)
        .in(monitoredServiceIdentifiers)
        .asList();
  }

  @Override
  public SLOHealthIndicator getBySLOIdentifier(ProjectParams projectParams, String serviceLevelObjectiveIdentifier) {
    return hPersistence.createQuery(SLOHealthIndicator.class)
        .filter(SLOHealthIndicatorKeys.accountId, projectParams.getAccountIdentifier())
        .filter(SLOHealthIndicatorKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(SLOHealthIndicatorKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .filter(SLOHealthIndicatorKeys.serviceLevelObjectiveIdentifier, serviceLevelObjectiveIdentifier)
        .get();
  }

  @Override
  public List<SLOHealthIndicator> getBySLOIdentifiers(
      ProjectParams projectParams, List<String> serviceLevelObjectiveIdentifiers) {
    return hPersistence.createQuery(SLOHealthIndicator.class)
        .filter(SLOHealthIndicatorKeys.accountId, projectParams.getAccountIdentifier())
        .filter(SLOHealthIndicatorKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(SLOHealthIndicatorKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .field(SLOHealthIndicatorKeys.serviceLevelObjectiveIdentifier)
        .in(serviceLevelObjectiveIdentifiers)
        .asList();
  }

  @Override
  public void upsert(ServiceLevelIndicator serviceLevelIndicator) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(serviceLevelIndicator.getAccountId())
                                      .orgIdentifier(serviceLevelIndicator.getOrgIdentifier())
                                      .projectIdentifier(serviceLevelIndicator.getProjectIdentifier())
                                      .build();
    ServiceLevelObjective serviceLevelObjective =
        serviceLevelObjectiveService.getFromSLIIdentifier(projectParams, serviceLevelIndicator.getIdentifier());
    SLOHealthIndicator sloHealthIndicator = getBySLOIdentifier(projectParams, serviceLevelObjective.getIdentifier());
    LocalDateTime currentLocalDate = LocalDateTime.ofInstant(clock.instant(), serviceLevelObjective.getZoneOffset());
    int totalErrorBudgetMinutes = serviceLevelObjective.getTotalErrorBudgetMinutes(currentLocalDate);
    ServiceLevelObjective.TimePeriod timePeriod = serviceLevelObjective.getCurrentTimeRange(currentLocalDate);
    Instant currentTimeMinute = DateTimeUtils.roundDownTo1MinBoundary(clock.instant());
    SLOGraphData sloGraphData = sliRecordService.getGraphData(serviceLevelIndicator.getUuid(),
        timePeriod.getStartTime(serviceLevelObjective.getZoneOffset()), currentTimeMinute, totalErrorBudgetMinutes,
        serviceLevelIndicator.getSliMissingDataType(), serviceLevelIndicator.getVersion());
    if (Objects.isNull(sloHealthIndicator)) {
      SLOHealthIndicator newSloHealthIndicator =
          SLOHealthIndicator.builder()
              .accountId(serviceLevelObjective.getAccountId())
              .orgIdentifier(serviceLevelObjective.getOrgIdentifier())
              .projectIdentifier(serviceLevelIndicator.getProjectIdentifier())
              .serviceLevelObjectiveIdentifier(serviceLevelObjective.getIdentifier())
              .monitoredServiceIdentifier(serviceLevelObjective.getMonitoredServiceIdentifier())
              .errorBudgetRemainingPercentage(sloGraphData.getErrorBudgetRemainingPercentage())
              .build();
      hPersistence.save(newSloHealthIndicator);
    } else {
      UpdateOperations<SLOHealthIndicator> updateOperations =
          hPersistence.createUpdateOperations(SLOHealthIndicator.class);
      updateOperations.set(
          SLOHealthIndicatorKeys.errorBudgetRemainingPercentage, sloGraphData.getErrorBudgetRemainingPercentage());
      updateOperations.set(SLOHealthIndicatorKeys.errorBudgetRisk,
          ErrorBudgetRisk.getFromPercentage(sloGraphData.getErrorBudgetRemainingPercentage()));
      updateOperations.set(SLOHealthIndicatorKeys.lastComputedAt, Instant.now());
      hPersistence.update(sloHealthIndicator, updateOperations);
    }
  }
}
