/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.entities;

import io.harness.annotation.StoreIn;
import io.harness.beans.EmbeddedUser;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@StoreIn(DbAliases.CENG)
@FieldNameConstants(innerTypeName = "CEViewKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceView", noClassnameStored = true)
@Schema(description = "This object will contain the complete definition of a Cloud Cost Perspective")
public final class CEView implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess,
                                     CreatedByAware, UpdatedByAware {
  @Id String uuid;
  @Schema(description = "Name of the Perspective (must be between 1 and 32 characters long)")
  @Size(min = 1, max = 32, message = "for view must be between 1 and 32 characters long")
  @NotBlank
  String name;
  String accountId;
  @Hidden @NotBlank String viewVersion;

  @Schema(description = "The time interval on which you want to create a Perspective") ViewTimeRange viewTimeRange;
  @Schema(description = "An array of Perspective Rules, which are combined using OR operator") List<ViewRule> viewRules;
  List<ViewFieldIdentifier> dataSources;
  @Schema(
      description =
          "Perspective default visualization, which specifies the default group by field, chart granularity (day / month) and chart type (line chart / bar chart)")
  ViewVisualization viewVisualization;
  @Hidden ViewType viewType = ViewType.CUSTOMER;

  @Hidden ViewState viewState = ViewState.DRAFT;

  @Hidden double totalCost;
  @Hidden long createdAt;
  @Hidden long lastUpdatedAt;
  @Hidden private EmbeddedUser createdBy;
  @Hidden private EmbeddedUser lastUpdatedBy;
}
