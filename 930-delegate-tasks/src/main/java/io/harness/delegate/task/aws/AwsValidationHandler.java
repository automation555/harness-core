package io.harness.delegate.task.aws;

import static io.harness.aws.AwsExceptionHandler.handleAmazonClientException;
import static io.harness.aws.AwsExceptionHandler.handleAmazonServiceException;

import io.harness.aws.AwsClient;
import io.harness.aws.AwsConfig;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsValidationParams;
import io.harness.delegate.task.ConnectorValidationHandler;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;

public class AwsValidationHandler implements ConnectorValidationHandler {
  @Inject SecretDecryptionService decryptionService;
  @Inject AwsNgConfigMapper ngConfigMapper;
  @Inject private AwsClient awsClient;
  @Inject private NGErrorHelper ngErrorHelper;

  @Override
  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    final AwsValidationParams awsValidationParams = (AwsValidationParams) connectorValidationParams;
    final AwsConnectorDTO connectorDTO = awsValidationParams.getAwsConnectorDTO();
    final List<EncryptedDataDetail> encryptedDataDetails = awsValidationParams.getEncryptedDataDetails();
    final List<DecryptableEntity> decryptableEntityList = connectorDTO.getDecryptableEntities();

    for (DecryptableEntity entity : decryptableEntityList) {
      decryptionService.decrypt(entity, encryptedDataDetails);
    }

    AwsConfig awsConfig = ngConfigMapper.mapAwsConfigWithDecryption(
        connectorDTO.getCredential(), connectorDTO.getCredential().getAwsCredentialType(), encryptedDataDetails);

    ConnectorValidationResult connectorValidationResult;
    try {
      connectorValidationResult = handleValidateTask(awsConfig);
    } catch (Exception e) {
      String errorMessage = e.getMessage();
      connectorValidationResult = ConnectorValidationResult.builder()
                                      .status(ConnectivityStatus.FAILURE)
                                      .errors(Collections.singletonList(ngErrorHelper.createErrorDetail(errorMessage)))
                                      .errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
                                      .testedAt(System.currentTimeMillis())
                                      .build();
    }

    return connectorValidationResult;
  }

  private ConnectorValidationResult handleValidateTask(AwsConfig awsConfig) {
    ConnectorValidationResult result = null;
    try {
      awsClient.validateAwsAccountCredential(awsConfig);
      result = ConnectorValidationResult.builder()
                   .status(ConnectivityStatus.SUCCESS)
                   .testedAt(System.currentTimeMillis())
                   .build();

    } catch (AmazonEC2Exception amazonEC2Exception) {
      handleAmazonServiceException(amazonEC2Exception);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return result;
  }
}
