package io.harness.delegate.task.aws;

import static io.harness.rule.OwnerRule.MEENAKSHI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;

import io.harness.CategoryTest;
import io.harness.aws.AwsClient;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsValidationParams;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AwsValidationHandlerTest extends CategoryTest {
  @Mock private SecretDecryptionService secretDecryptionService;
  @Mock private NGErrorHelper ngErrorHelper;
  @Mock private AwsClient awsClient;
  @Mock private AwsNgConfigMapper ngConfigMapper;
  @InjectMocks AwsValidationHandler awsValidationHandler;
  private final String accountIdentifier = "accountIdentifier";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MEENAKSHI)
  @Category(UnitTests.class)
  public void testValidateSuccess() {
    String secretKeyRefIdentifier = "secretKeyRefIdentifier";
    String secretKey = "secretKey";
    SecretRefData passwordSecretRef = SecretRefData.builder()
                                          .identifier(secretKeyRefIdentifier)
                                          .scope(Scope.ACCOUNT)
                                          .decryptedValue(secretKey.toCharArray())
                                          .build();
    AwsManualConfigSpecDTO awsManualConfigSpecDTO =
        AwsManualConfigSpecDTO.builder().accessKey("testAccessKey").secretKeyRef(passwordSecretRef).build();

    AwsCredentialDTO awsCredentialDTO = AwsCredentialDTO.builder()
                                            .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
                                            .config(awsManualConfigSpecDTO)
                                            .build();

    AwsConnectorDTO awsConnectorDTO = AwsConnectorDTO.builder().credential(awsCredentialDTO).build();

    ConnectorValidationParams connectorValidationParams = AwsValidationParams.builder()
                                                              .awsConnectorDTO(awsConnectorDTO)
                                                              .connectorName("TestAWSName")
                                                              .encryptedDataDetails(null)
                                                              .build();

    ConnectorValidationResult result = awsValidationHandler.validate(connectorValidationParams, accountIdentifier);
    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
  }

  @Test
  @Owner(developers = MEENAKSHI)
  @Category(UnitTests.class)
  public void testValidateFailure() {
    doThrow(new RuntimeException("No Credentials found")).when(awsClient).validateAwsAccountCredential(any());
    String secretKeyRefIdentifier = "secretKeyRefIdentifier";
    String secretKey = "secretKey";
    SecretRefData passwordSecretRef = SecretRefData.builder()
                                          .identifier(secretKeyRefIdentifier)
                                          .scope(Scope.ACCOUNT)
                                          .decryptedValue(secretKey.toCharArray())
                                          .build();
    AwsManualConfigSpecDTO awsManualConfigSpecDTO =
        AwsManualConfigSpecDTO.builder().accessKey("testAccessKey").secretKeyRef(passwordSecretRef).build();

    AwsCredentialDTO awsCredentialDTO = AwsCredentialDTO.builder()
                                            .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
                                            .config(awsManualConfigSpecDTO)
                                            .build();

    AwsConnectorDTO awsConnectorDTO = AwsConnectorDTO.builder().credential(awsCredentialDTO).build();

    ConnectorValidationParams connectorValidationParams = AwsValidationParams.builder()
                                                              .awsConnectorDTO(awsConnectorDTO)
                                                              .connectorName("TestAWSName")
                                                              .encryptedDataDetails(null)
                                                              .build();

    ConnectorValidationResult result = awsValidationHandler.validate(connectorValidationParams, accountIdentifier);
    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
  }
}
