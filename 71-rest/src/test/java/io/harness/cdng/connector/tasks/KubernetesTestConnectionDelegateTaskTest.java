package io.harness.cdng.connector.tasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.cdng.connector.service.KubernetesConnectorDelegateService;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.UserNamePasswordDTO;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.TaskType;
import software.wings.service.intfc.security.EncryptionService;

import java.util.Collections;

public class KubernetesTestConnectionDelegateTaskTest extends WingsBaseTest {
  @Mock KubernetesConnectorDelegateService kubernetesConnectorDelegateService;
  @Mock EncryptionService encryptionService;

  KubernetesAuthDTO kubernetesAuthDTO =
      KubernetesAuthDTO.builder()
          .authType(KubernetesAuthType.USER_PASSWORD)
          .credentials(UserNamePasswordDTO.builder().username("username").password("password".toCharArray()).build())
          .build();

  @InjectMocks
  private KubernetesTestConnectionDelegateTask kubernetesTestConnectionDelegateTask =
      (KubernetesTestConnectionDelegateTask) TaskType.VALIDATE_KUBERNETES_CONFIG.getDelegateRunnableTask(
          DelegateTaskPackage.builder()
              .delegateId("delegateid")
              .delegateTask(
                  DelegateTask.builder()
                      .data(
                          (TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                              .parameters(new Object[] {
                                  KubernetesConnectionTaskParams.builder()
                                      .kubernetesClusterConfig(
                                          KubernetesClusterConfigDTO.builder()
                                              .config(
                                                  KubernetesClusterDetailsDTO.builder().auth(kubernetesAuthDTO).build())
                                              .build())
                                      .encryptionDetails(Collections.emptyList())
                                      .build()})
                              .build())
                      .build())
              .build(),
          notifyResponseData -> {}, () -> true);

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void run() {
    when(encryptionService.decrypt(any(), anyList())).thenReturn(kubernetesAuthDTO.getCredentials());
    kubernetesTestConnectionDelegateTask.run();
    verify(kubernetesConnectorDelegateService, times(1)).validate(any());
  }
}