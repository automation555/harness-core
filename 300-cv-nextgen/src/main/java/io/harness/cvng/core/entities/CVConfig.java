package io.harness.cvng.core.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import static com.google.common.base.Preconditions.checkNotNull;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.services.api.UpdatableEntity;
import io.harness.cvng.models.VerificationType;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.UpdateOperations;

@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "CVConfigKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "cvConfigs")
@HarnessEntity(exportable = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@StoreIn(DbAliases.CVNG)
public abstract class CVConfig
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess, PersistentRegularIterable {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("env_service_category_index")
                 .field(CVConfigKeys.accountId)
                 .field(CVConfigKeys.orgIdentifier)
                 .field(CVConfigKeys.projectIdentifier)
                 .field(CVConfigKeys.envIdentifier)
                 .field(CVConfigKeys.serviceIdentifier)
                 .build(),
            CompoundMongoIndex.builder()
                .name("insert_index")
                .field(CVConfigKeys.accountId)
                .field(CVConfigKeys.orgIdentifier)
                .field(CVConfigKeys.projectIdentifier)
                .field(CVConfigKeys.identifier)
                .build())
        .build();
  }

  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  @NotNull private VerificationType verificationType;

  @NotNull private String accountId;
  @NotNull @FdIndex private String connectorIdentifier;

  @NotNull private String serviceIdentifier;
  @NotNull private String envIdentifier;
  @NotNull private String projectIdentifier;
  @NotNull private String orgIdentifier;
  @NotNull private CVMonitoringCategory category;
  private boolean enabled;
  private String productName;
  @NotNull private String identifier;
  @NotNull private String monitoringSourceName;

  @FdIndex private Long createNextTaskIteration;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (fieldName.equals(CVConfigKeys.createNextTaskIteration)) {
      this.createNextTaskIteration = nextIteration;
      return;
    }

    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (fieldName.equals(CVConfigKeys.createNextTaskIteration)) {
      return createNextTaskIteration;
    }

    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public void validate() {
    checkNotNull(getVerificationType(), generateErrorMessageFromParam(CVConfigKeys.verificationType));
    checkNotNull(accountId, generateErrorMessageFromParam(CVConfigKeys.accountId));
    checkNotNull(connectorIdentifier, generateErrorMessageFromParam(CVConfigKeys.connectorIdentifier));
    checkNotNull(serviceIdentifier, generateErrorMessageFromParam(CVConfigKeys.serviceIdentifier));
    checkNotNull(envIdentifier, generateErrorMessageFromParam(CVConfigKeys.envIdentifier));
    checkNotNull(projectIdentifier, generateErrorMessageFromParam(CVConfigKeys.projectIdentifier));
    checkNotNull(identifier, generateErrorMessageFromParam(CVConfigKeys.identifier));
    checkNotNull(monitoringSourceName, generateErrorMessageFromParam(CVConfigKeys.monitoringSourceName));
    checkNotNull(category, generateErrorMessageFromParam(CVConfigKeys.category));
    validateParams();
  }

  protected abstract void validateParams();

  public abstract DataSourceType getType();

  public abstract TimeRange getFirstTimeDataCollectionTimeRange();

  @JsonIgnore public abstract String getDataCollectionDsl();
  public abstract boolean queueAnalysisForPreDeploymentTask();

  public abstract static class CVConfigUpdatableEntity<T extends CVConfig, D extends CVConfig>
      implements UpdatableEntity<T, D> {
    protected void setCommonOperations(UpdateOperations<T> updateOperations, D cvConfig) {
      updateOperations.set(CVConfigKeys.verificationType, cvConfig.getVerificationType())
          .set(CVConfigKeys.serviceIdentifier, cvConfig.getServiceIdentifier())
          .set(CVConfigKeys.envIdentifier, cvConfig.getEnvIdentifier())
          .set(CVConfigKeys.monitoringSourceName, cvConfig.getMonitoringSourceName())
          .set(CVConfigKeys.category, cvConfig.getCategory());
    }
  }
}
