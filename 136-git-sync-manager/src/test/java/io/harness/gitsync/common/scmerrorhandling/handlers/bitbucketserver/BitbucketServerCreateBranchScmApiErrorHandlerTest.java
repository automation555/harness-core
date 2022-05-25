/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.scmerrorhandling.handlers.bitbucketserver;

import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.exception.*;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class BitbucketServerCreateBranchScmApiErrorHandlerTest extends GitSyncTestBase {
  @Inject BitbucketServerCreateBranchScmApiErrorHandler bitbucketServerCreateBranchScmApiErrorHandler;

  private static final String errorMessage = "errorMessage";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testHandleErrorOnUnauthorizedResponse() {
    try {
      bitbucketServerCreateBranchScmApiErrorHandler.handleError(401, errorMessage);
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmUnauthorizedException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testHandleErrorOnUnauthenticatedResponse() {
    try {
      bitbucketServerCreateBranchScmApiErrorHandler.handleError(403, errorMessage);
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmUnauthorizedException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testHandleErrorOnResourceNotFoundResponse() {
    try {
      bitbucketServerCreateBranchScmApiErrorHandler.handleError(404, errorMessage);
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmBadRequestException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testHandleErrorOnBadRequestEntityResponse() {
    try {
      bitbucketServerCreateBranchScmApiErrorHandler.handleError(400, errorMessage);
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmBadRequestException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testHandleErrorOnAlreadyExistsResponse() {
    try {
      bitbucketServerCreateBranchScmApiErrorHandler.handleError(409, errorMessage);
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmBadRequestException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testHandleErrorWhenUnexpectedStatusCode() {
    try {
      bitbucketServerCreateBranchScmApiErrorHandler.handleError(405, errorMessage);
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmUnexpectedException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }
}
