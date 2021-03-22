package io.harness.chartmuseum;

import static io.harness.chartmuseum.ChartMuseumConstants.AWS_ACCESS_KEY_ID;
import static io.harness.chartmuseum.ChartMuseumConstants.AWS_SECRET_ACCESS_KEY;
import static io.harness.chartmuseum.ChartMuseumConstants.GOOGLE_APPLICATION_CREDENTIALS;
import static io.harness.rule.OwnerRule.ABOSII;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.zeroturnaround.exec.StartedProcess;

public class ChartMuseumClientHelperTest extends CategoryTest {
  private static final String CHARTMUSEUM_BIN_PATH = "/usr/bin/chartmuseum";

  @Mock private Process process;
  @Mock private StartedProcess startedProcess;
  @Mock private K8sGlobalConfigService k8sGlobalConfigService;

  @InjectMocks @Spy private ChartMuseumClientHelper clientHelper;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    doReturn(CHARTMUSEUM_BIN_PATH).when(k8sGlobalConfigService).getChartMuseumPath();
    doReturn(startedProcess)
        .when(clientHelper)
        .startProcess(anyString(), anyMapOf(String.class, String.class), any(StringBuffer.class));
    doReturn(process).when(startedProcess).getProcess();
    doReturn(true).when(process).isAlive();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testStartS3ChartMuseumServer() throws Exception {
    final String bucketName = "s3-bucket";
    final String basePath = "charts";
    final String region = "us-west1";

    ChartMuseumServer startedServer =
        clientHelper.startS3ChartMuseumServer(bucketName, basePath, region, true, null, null);
    assertThat(startedServer.getStartedProcess()).isEqualTo(startedProcess);
    ArgumentCaptor<String> commandCaptor = ArgumentCaptor.forClass(String.class);

    verify(clientHelper, times(1))
        .startProcess(commandCaptor.capture(), eq(Collections.emptyMap()), any(StringBuffer.class));

    String command = commandCaptor.getValue();
    assertThat(command).contains(
        format("--storage=amazon --storage-amazon-bucket=%s --storage-amazon-prefix=%s --storage-amazon-region=%s",
            bucketName, basePath, region));
    assertThat(command).doesNotContain("--port=${PORT}");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testStartGCSChartMuseumServer() throws Exception {
    final String bucketName = "gcs-bucket";
    final String basePath = "charts";
    final String resourceDirectory = "resources";
    final char[] serviceAccountKey = "service-account-key".toCharArray();
    final String credentialsFilePath = "resources/credentials.json";

    doReturn(credentialsFilePath).when(clientHelper).writeGCSCredentialsFile(resourceDirectory, serviceAccountKey);

    ChartMuseumServer startedServer =
        clientHelper.startGCSChartMuseumServer(bucketName, basePath, serviceAccountKey, resourceDirectory);
    assertThat(startedServer.getStartedProcess()).isEqualTo(startedProcess);
    ArgumentCaptor<String> commandCaptor = ArgumentCaptor.forClass(String.class);

    verify(clientHelper, times(1))
        .startProcess(commandCaptor.capture(), eq(ImmutableMap.of(GOOGLE_APPLICATION_CREDENTIALS, credentialsFilePath)),
            any(StringBuffer.class));

    String command = commandCaptor.getValue();
    assertThat(command).contains(
        format("--storage=google --storage-google-bucket=%s --storage-google-prefix=%s", bucketName, basePath));
    assertThat(command).doesNotContain("--port=${PORT}");
    assertThat(true).isTrue();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testStartServerFailed() {
    doReturn(false).when(process).isAlive();

    assertThatThrownBy(() -> clientHelper.startServer("start server", Collections.emptyMap()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Failed after 5 retries");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getEnvForAwsConfig() {
    testGetEnvForAwsConfig();
    testGetEnvForAwsConfigWithAssumeDelegateRole();
  }

  private void testGetEnvForAwsConfigWithAssumeDelegateRole() {
    Map<String, String> env = ChartMuseumClientHelper.getEnvForAwsConfig(null, null, true);
    assertThat(env).isEmpty();
  }

  private void testGetEnvForAwsConfig() {
    String accessKey = "access-key";
    String secretKey = "secret-key";
    Map<String, String> env =
        ChartMuseumClientHelper.getEnvForAwsConfig(accessKey.toCharArray(), secretKey.toCharArray(), false);
    assertThat(env.keySet()).hasSize(2);
    assertThat(env.get(AWS_ACCESS_KEY_ID)).isEqualTo(accessKey);
    assertThat(env.get(AWS_SECRET_ACCESS_KEY)).isEqualTo(secretKey);
  }
}