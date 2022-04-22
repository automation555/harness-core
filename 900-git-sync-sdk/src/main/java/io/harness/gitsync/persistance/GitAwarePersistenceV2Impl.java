/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.persistance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.exception.EntityNotFoundException;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.scm.SCMGitSyncHelper;
import io.harness.gitsync.scm.beans.ScmGetFileResponse;
import io.harness.gitsync.scm.beans.ScmGitMetaData;
import io.harness.gitsync.v2.GitAware;
import io.harness.gitsync.v2.StoreType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Singleton
@OwnedBy(HarnessTeam.PL)
public class GitAwarePersistenceV2Impl implements GitAwarePersistenceV2 {
  @Inject private Map<String, GitSdkEntityHandlerInterface> gitPersistenceHelperServiceMap;
  @Inject private GitAwarePersistence gitAwarePersistence;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private SCMGitSyncHelper scmGitSyncHelper;

  @Override
  public Optional<GitAware> findOne(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Class entityClass, Criteria criteria) {
    Optional<GitAware> savedEntityOptional =
        gitAwarePersistence.findOne(criteria, projectIdentifier, orgIdentifier, accountIdentifier, entityClass);
    if (savedEntityOptional.isPresent()) {
      GitAware savedEntity = savedEntityOptional.get();
      GitContextHelper.updateScmGitMetaData(ScmGitMetaData.builder()
                                                .repoName(savedEntity.getRepo())
                                                .branchName(savedEntity.getBranch())
                                                .blobId(savedEntity.getObjectIdOfYaml())
                                                .filePath(savedEntity.getFilePath())
                                                .build());
      return savedEntityOptional;
    }

    Criteria gitAwareCriteria = Criteria.where(getGitSdkEntityHandlerInterface(entityClass).getStoreTypeKey())
                                    .in(Arrays.asList(StoreType.values()));
    Query query = new Query().addCriteria(new Criteria().andOperator(criteria, gitAwareCriteria));
    final GitAware savedEntity = (GitAware) mongoTemplate.findOne(query, entityClass);
    if (savedEntity == null) {
      throw new EntityNotFoundException(
          String.format("No entity found for accountIdentifier : %s, orgIdentifier : %s , projectIdentifier : %s",
              accountIdentifier, orgIdentifier, projectIdentifier));
    }

    if (savedEntity.getStoreType() == StoreType.REMOTE) {
      // fetch yaml from git
      GitEntityInfo gitEntityInfo = GitContextHelper.getGitEntityInfoV2();
      ScmGetFileResponse scmGetFileResponse = scmGitSyncHelper.getFile(Scope.builder()
                                                                           .accountIdentifier(accountIdentifier)
                                                                           .orgIdentifier(orgIdentifier)
                                                                           .projectIdentifier(projectIdentifier)
                                                                           .build(),
          savedEntity.getRepo(), gitEntityInfo.getBranch(), gitEntityInfo.getFilePath(), gitEntityInfo.getCommitId(),
          savedEntity.getConnectorRef(), Collections.emptyMap());
      savedEntity.setData(scmGetFileResponse.getFileContent());
      GitContextHelper.updateScmGitMetaData(scmGetFileResponse.getGitMetaData());
    }

    return Optional.of(savedEntity);
  }

  private GitSdkEntityHandlerInterface getGitSdkEntityHandlerInterface(Class entityClass) {
    return gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName());
  }
}
