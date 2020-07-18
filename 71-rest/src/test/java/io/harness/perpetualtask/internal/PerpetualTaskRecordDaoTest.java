package io.harness.perpetualtask.internal;

import static io.harness.rule.OwnerRule.HITESH;
import static io.harness.rule.OwnerRule.VUK;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;
import com.google.protobuf.Any;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.instancesync.AwsSshInstanceSyncPerpetualTaskParams;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PerpetualTaskRecordDaoTest extends WingsBaseTest {
  @InjectMocks @Inject private PerpetualTaskRecordDao perpetualTaskRecordDao;

  private final String CLUSTER_ID = "clusterId";
  private final String CLUSTER_NAME = "clusterName";
  private final String ACCOUNT_ID = "test-account-id";
  private final String DELEGATE_ID = "test-delegate-id1";
  private final String CLOUD_PROVIDER_ID = "cloudProviderId";
  private final long HEARTBEAT_MILLIS = Instant.now().toEpochMilli();

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testAppointDelegate() {
    long lastContextUpdated = 1L;
    String taskId = perpetualTaskRecordDao.save(PerpetualTaskRecord.builder().accountId(ACCOUNT_ID).build());
    perpetualTaskRecordDao.appointDelegate(taskId, DELEGATE_ID, lastContextUpdated);
    PerpetualTaskRecord task = perpetualTaskRecordDao.getTask(taskId);
    assertThat(task).isNotNull();
    assertThat(task.getDelegateId()).isEqualTo(DELEGATE_ID);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testResetDelegateIdForTaskWithClientParams() {
    String state = UUIDGenerator.generateUuid();

    PerpetualTaskClientContext clientContext = getClientContext();
    PerpetualTaskRecord perpetualTaskRecord = getPerpetualTaskRecord();
    perpetualTaskRecord.setClientContext(clientContext);

    String taskId = perpetualTaskRecordDao.save(perpetualTaskRecord);
    perpetualTaskRecordDao.resetDelegateIdForTask(ACCOUNT_ID, taskId, state, null);
    PerpetualTaskRecord task = perpetualTaskRecordDao.getTask(taskId);

    assertThat(task).isNotNull();
    assertThat(task.getDelegateId()).isEqualTo("");
    assertThat(task.getClientContext()).isEqualTo(clientContext);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testResetDelegateIdForTaskWithTaskParams() {
    String state = UUIDGenerator.generateUuid();

    PerpetualTaskClientContext clientContext = getClientContextWithTaskParams();
    PerpetualTaskRecord perpetualTaskRecord = getPerpetualTaskRecord();
    perpetualTaskRecord.setClientContext(clientContext);

    String taskId = perpetualTaskRecordDao.save(perpetualTaskRecord);

    perpetualTaskRecordDao.resetDelegateIdForTask(ACCOUNT_ID, taskId, state, null);
    PerpetualTaskRecord task = perpetualTaskRecordDao.getTask(taskId);
    assertThat(task).isNotNull();
    assertThat(task.getDelegateId()).isEqualTo("");
    assertThat(task.getClientContext().getExecutionBundle()).isNull();

    PerpetualTaskExecutionBundle executionBundle =
        PerpetualTaskExecutionBundle.newBuilder()
            .setTaskParams(Any.pack(AwsSshInstanceSyncPerpetualTaskParams.getDefaultInstance()))
            .build();
    perpetualTaskRecordDao.resetDelegateIdForTask(ACCOUNT_ID, taskId, state, executionBundle);

    task = perpetualTaskRecordDao.getTask(taskId);
    assertThat(task).isNotNull();
    assertThat(task.getDelegateId()).isEqualTo("");
    assertThat(task.getClientContext().getExecutionBundle()).isEqualTo(executionBundle.toByteArray());
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testRemovePerpetualTask() {
    String taskId = perpetualTaskRecordDao.save(getPerpetualTaskRecord());
    perpetualTaskRecordDao.remove(ACCOUNT_ID, taskId);
    PerpetualTaskRecord task = perpetualTaskRecordDao.getTask(taskId);
    assertThat(task).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testSaveHeartbeat() {
    String taskId = perpetualTaskRecordDao.save(getPerpetualTaskRecord());
    PerpetualTaskRecord task = perpetualTaskRecordDao.getTask(taskId);
    boolean saveHeartbeat = perpetualTaskRecordDao.saveHeartbeat(task, HEARTBEAT_MILLIS);
    assertThat(saveHeartbeat).isTrue();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskRecordDao.getTask(taskId);
    assertThat(perpetualTaskRecord).isNotNull();
    assertThat(perpetualTaskRecord.getLastHeartbeat()).isEqualTo(HEARTBEAT_MILLIS);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testListAssignedTasks() {
    PerpetualTaskRecord taskRecord = getPerpetualTaskRecord();
    perpetualTaskRecordDao.save(taskRecord);
    List<PerpetualTaskRecord> taskIds = perpetualTaskRecordDao.listAssignedTasks(DELEGATE_ID, ACCOUNT_ID);
    assertThat(taskIds).hasSize(1);
    for (PerpetualTaskRecord record : taskIds) {
      assertThat(record.getClientContext()).isNotNull();
      assertThat(record.getUuid()).isEqualTo(taskRecord.getUuid());
      assertThat(record.getClientContext().getLastContextUpdated())
          .isEqualTo(taskRecord.getClientContext().getLastContextUpdated());
    }
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testGetExistingPerpetualTaskWithTaskParams() {
    PerpetualTaskClientContext clientContext = getClientContextWithTaskParams();
    PerpetualTaskRecord perpetualTaskRecord = getPerpetualTaskRecord();
    perpetualTaskRecord.setClientContext(clientContext);
    perpetualTaskRecordDao.save(perpetualTaskRecord);
    Optional<PerpetualTaskRecord> existingPerpetualTask =
        perpetualTaskRecordDao.getExistingPerpetualTask(ACCOUNT_ID, PerpetualTaskType.K8S_WATCH, clientContext);
    PerpetualTaskRecord savedPerpetualTaskRecord = existingPerpetualTask.get();
    assertThat(savedPerpetualTaskRecord).isNotNull();
    assertThat(savedPerpetualTaskRecord.getClientContext().getExecutionBundle())
        .isEqualTo(clientContext.getExecutionBundle());
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetExistingPerpetualTaskWithClientParams() {
    PerpetualTaskClientContext clientContext = getClientContext();
    PerpetualTaskRecord perpetualTaskRecord = getPerpetualTaskRecord();
    perpetualTaskRecord.setClientContext(clientContext);
    perpetualTaskRecordDao.save(perpetualTaskRecord);
    Optional<PerpetualTaskRecord> existingPerpetualTask =
        perpetualTaskRecordDao.getExistingPerpetualTask(ACCOUNT_ID, PerpetualTaskType.K8S_WATCH, clientContext);
    PerpetualTaskRecord savedPerpetualTaskRecord = existingPerpetualTask.get();
    assertThat(savedPerpetualTaskRecord).isNotNull();
    assertThat(savedPerpetualTaskRecord.getClientContext().getClientParams())
        .isEqualTo(clientContext.getClientParams());
  }

  public PerpetualTaskClientContext getClientContext() {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put(CLOUD_PROVIDER_ID, CLOUD_PROVIDER_ID);
    clientParamMap.put(CLUSTER_ID, CLUSTER_ID);
    clientParamMap.put(CLUSTER_NAME, CLUSTER_NAME);
    return PerpetualTaskClientContext.builder().clientParams(clientParamMap).build();
  }

  public PerpetualTaskClientContext getClientContextWithTaskParams() {
    byte[] taskParameters = new byte[] {1, 2, 3, 4};
    return PerpetualTaskClientContext.builder().executionBundle(taskParameters).build();
  }

  public PerpetualTaskRecord getPerpetualTaskRecord() {
    return PerpetualTaskRecord.builder()
        .accountId(ACCOUNT_ID)
        .perpetualTaskType(PerpetualTaskType.K8S_WATCH)
        .clientContext(getClientContext())
        .delegateId(DELEGATE_ID)
        .build();
  }
}
