/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.filestore.custom;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

@OwnedBy(CDP)
public interface FileStoreRepositoryCustom {
  /**
   * Execute aggregation query.
   *
   * @param aggregation aggregation query
   * @param classToFillResultIn cast result in class
   * @param <T>
   * @return the aggregation results
   */
  <T> AggregationResults<T> aggregate(Aggregation aggregation, Class<T> classToFillResultIn);
}
