package software.wings.service.impl.aws.manager;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.exception.WingsException;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.exception.InvalidRequestException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsCodeDeployListAppRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployListAppResponse;
import software.wings.service.impl.aws.model.AwsCodeDeployListAppRevisionRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployListAppRevisionResponse;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentConfigRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentConfigResponse;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentGroupRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentGroupResponse;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentInstancesRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentInstancesResponse;
import software.wings.service.impl.aws.model.AwsCodeDeployRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployS3LocationData;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.aws.manager.AwsCodeDeployHelperServiceManager;
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class AwsCodeDeployHelperServiceManagerImpl implements AwsCodeDeployHelperServiceManager {
  private static final long TIME_OUT_IN_MINUTES = 2;
  @Inject private DelegateService delegateService;

  @Override
  public List<String> listApplications(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsCodeDeployListAppRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .build());
    return ((AwsCodeDeployListAppResponse) response).getApplications();
  }

  @Override
  public List<String> listDeploymentConfiguration(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String region) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsCodeDeployListDeploymentConfigRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptedDataDetails)
            .region(region)
            .build());
    return ((AwsCodeDeployListDeploymentConfigResponse) response).getDeploymentConfig();
  }

  @Override
  public List<String> listDeploymentGroups(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String region, String appName) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsCodeDeployListDeploymentGroupRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptedDataDetails)
            .region(region)
            .appName(appName)
            .build());
    return ((AwsCodeDeployListDeploymentGroupResponse) response).getDeploymentGroups();
  }

  @Override
  public List<Instance> listDeploymentInstances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String region, String deploymentId) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsCodeDeployListDeploymentInstancesRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptedDataDetails)
            .region(region)
            .deploymentId(deploymentId)
            .build());
    return ((AwsCodeDeployListDeploymentInstancesResponse) response).getInstances();
  }

  @Override
  public AwsCodeDeployS3LocationData listAppRevision(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String region, String appName, String deploymentGroupName) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsCodeDeployListAppRevisionRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptedDataDetails)
            .region(region)
            .appName(appName)
            .deploymentGroupName(deploymentGroupName)
            .build());
    return ((AwsCodeDeployListAppRevisionResponse) response).getS3LocationData();
  }

  private AwsResponse executeTask(String accountId, AwsCodeDeployRequest request) {
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.AWS_CODE_DEPLOY_TASK)
                                    .withAccountId(accountId)
                                    .withAppId(GLOBAL_APP_ID)
                                    .withAsync(false)
                                    .withTimeout(TimeUnit.MINUTES.toMillis(TIME_OUT_IN_MINUTES))
                                    .withParameters(new Object[] {request})
                                    .build();
    try {
      NotifyResponseData notifyResponseData = delegateService.executeTask(delegateTask);
      if (notifyResponseData instanceof ErrorNotifyResponseData) {
        throw new WingsException(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
      }
      return (AwsResponse) notifyResponseData;
    } catch (InterruptedException ex) {
      throw new InvalidRequestException(ex.getMessage(), WingsException.USER);
    }
  }
}