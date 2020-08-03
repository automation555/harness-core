package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskDetails;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.SecretDetail;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.artifact.ArtifactFileMetadata;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.gitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConnectionType;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitSSHAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitSyncConfig;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskResponse;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesOpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.delegate.beans.executioncapability.AlwaysFalseValidationCapability;
import io.harness.delegate.beans.executioncapability.AwsRegionCapability;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ChartMuseumCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.delegate.beans.git.EntityScope;
import io.harness.delegate.beans.git.GitCommand;
import io.harness.delegate.beans.git.GitCommand.GitCommandType;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.git.GitCommandRequest;
import io.harness.delegate.beans.git.GitCommandResult;
import io.harness.delegate.beans.git.GitCommitAndPushRequest;
import io.harness.delegate.beans.git.GitCommitAndPushResult;
import io.harness.delegate.beans.git.GitCommitResult;
import io.harness.delegate.beans.git.GitFileChange;
import io.harness.delegate.beans.git.GitPushResult;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.delegate.command.CommandExecutionData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.exception.ArtifactServerException;
import io.harness.delegate.exception.DelegateRetryableException;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.delegate.task.aws.AwsElbListenerRuleData;
import io.harness.delegate.task.aws.AwsLoadBalancerDetails;
import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.delegate.task.aws.LoadBalancerType;
import io.harness.delegate.task.azure.request.AzureVMSSGetVirtualMachineScaleSetParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListResourceGroupsNamesParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListSubscriptionsParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListVirtualMachineScaleSetsParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType;
import io.harness.delegate.task.azure.response.AzureVMSSGetVirtualMachineScaleSetResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListResourceGroupsNamesResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListSubscriptionsResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListVirtualMachineScaleSetsResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskResponse;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.delegate.task.shell.ShellScriptApprovalTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstDeployTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstGetElastigroupJsonParameters;
import io.harness.delegate.task.spotinst.request.SpotInstListElastigroupInstancesParameters;
import io.harness.delegate.task.spotinst.request.SpotInstListElastigroupNamesParameters;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType;
import io.harness.delegate.task.spotinst.request.SpotinstTrafficShiftAlbDeployParameters;
import io.harness.delegate.task.spotinst.request.SpotinstTrafficShiftAlbSetupParameters;
import io.harness.delegate.task.spotinst.request.SpotinstTrafficShiftAlbSwapRoutesParameters;
import io.harness.delegate.task.spotinst.response.SpotInstDeployTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotInstGetElastigroupJsonResponse;
import io.harness.delegate.task.spotinst.response.SpotInstListElastigroupInstancesResponse;
import io.harness.delegate.task.spotinst.response.SpotInstListElastigroupNamesResponse;
import io.harness.delegate.task.spotinst.response.SpotInstSetupTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotinstTrafficShiftAlbDeployResponse;
import io.harness.delegate.task.spotinst.response.SpotinstTrafficShiftAlbSetupResponse;
import io.harness.serializer.KryoRegistrar;

public class DelegateTasksBeansKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(DelegateRetryableException.class, 5521);
    kryo.register(ShellScriptApprovalTaskParameters.class, 20001);
    kryo.register(HttpTaskParameters.class, 20002);
    kryo.register(ScriptType.class, 5253);
    kryo.register(AwsElbListener.class, 5600);
    kryo.register(CommandExecutionData.class, 5035);
    kryo.register(CommandExecutionResult.class, 5036);
    kryo.register(SecretDetail.class, 19001);
    kryo.register(TaskData.class, 19002);
    kryo.register(HttpConnectionExecutionCapability.class, 19003);
    kryo.register(CapabilityType.class, 19004);
    kryo.register(DelegateMetaInfo.class, 5372);
    kryo.register(DelegateTaskNotifyResponseData.class, 5373);
    kryo.register(DelegateTaskResponse.ResponseCode.class, 5520);
    kryo.register(DelegateTaskResponse.class, 5006);
    kryo.register(ProcessExecutorCapability.class, 19007);
    kryo.register(AwsRegionCapability.class, 19008);
    kryo.register(RemoteMethodReturnValueData.class, 5122);
    kryo.register(SocketConnectivityExecutionCapability.class, 19009);
    kryo.register(SpotInstTaskParameters.class, 19011);
    kryo.register(SpotInstSetupTaskParameters.class, 19012);
    kryo.register(SpotInstTaskType.class, 19013);
    kryo.register(SpotInstTaskExecutionResponse.class, 19014);
    kryo.register(SpotInstTaskResponse.class, 19015);
    kryo.register(SpotInstSetupTaskResponse.class, 19016);
    kryo.register(SpotInstDeployTaskResponse.class, 19017);
    kryo.register(SpotInstDeployTaskParameters.class, 19018);
    kryo.register(SystemEnvCheckerCapability.class, 19022);
    kryo.register(SpotInstSwapRoutesTaskParameters.class, 19023);
    kryo.register(ErrorNotifyResponseData.class, 5213);
    kryo.register(AwsLoadBalancerDetails.class, 19024);
    kryo.register(SpotInstGetElastigroupJsonParameters.class, 19025);
    kryo.register(SpotInstListElastigroupInstancesParameters.class, 19026);
    kryo.register(SpotInstListElastigroupNamesParameters.class, 19027);
    kryo.register(SpotInstGetElastigroupJsonResponse.class, 19028);
    kryo.register(SpotInstListElastigroupInstancesResponse.class, 19029);
    kryo.register(SpotInstListElastigroupNamesResponse.class, 19030);
    kryo.register(LoadBalancerDetailsForBGDeployment.class, 19031);
    kryo.register(LoadBalancerType.class, 19032);
    kryo.register(ArtifactServerException.class, 7244);
    kryo.register(PcfManifestsPackage.class, 19033);
    kryo.register(ArtifactFileMetadata.class, 19034);
    kryo.register(AwsElbListenerRuleData.class, 19035);
    kryo.register(AlwaysFalseValidationCapability.class, 19036);
    kryo.register(LbDetailsForAlbTrafficShift.class, 19037);
    kryo.register(ChartMuseumCapability.class, 19038);
    kryo.register(SpotinstTrafficShiftAlbSetupParameters.class, 19039);
    kryo.register(SpotinstTrafficShiftAlbSetupResponse.class, 19040);
    kryo.register(SpotinstTrafficShiftAlbDeployParameters.class, 19041);
    kryo.register(SpotinstTrafficShiftAlbDeployResponse.class, 19042);
    kryo.register(SpotinstTrafficShiftAlbSwapRoutesParameters.class, 19043);
    kryo.register(DelegateTaskDetails.class, 19044);
    kryo.register(KubernetesClusterConfigDTO.class, 19045);
    kryo.register(KubernetesCredentialType.class, 19046);
    kryo.register(KubernetesCredentialDTO.class, 19047);
    kryo.register(KubernetesDelegateDetailsDTO.class, 19048);
    kryo.register(KubernetesClusterDetailsDTO.class, 19049);
    kryo.register(KubernetesAuthDTO.class, 19050);
    kryo.register(KubernetesAuthType.class, 19051);
    kryo.register(KubernetesUserNamePasswordDTO.class, 19052);
    kryo.register(KubernetesClientKeyCertDTO.class, 19053);
    kryo.register(KubernetesServiceAccountDTO.class, 19054);
    kryo.register(KubernetesOpenIdConnectDTO.class, 19055);
    kryo.register(KubernetesConnectionTaskResponse.class, 19056);
    kryo.register(KubernetesConnectionTaskParams.class, 19057);
    kryo.register(KubernetesAuthCredentialDTO.class, 19058);
    kryo.register(K8sTaskType.class, 7125);
    kryo.register(ConnectorValidationResult.class, 19059);
    kryo.register(GitConfigDTO.class, 19060);
    kryo.register(GitCommandParams.class, 19061);
    kryo.register(GitCommand.class, 19062);
    kryo.register(GitAuthenticationDTO.class, 19063);
    kryo.register(GitHTTPAuthenticationDTO.class, 19064);
    kryo.register(GitSSHAuthenticationDTO.class, 19065);
    kryo.register(GitAuthType.class, 19066);
    kryo.register(GitCommandExecutionResponse.class, 19067);
    kryo.register(GitConnectionType.class, 19068);
    kryo.register(GitSyncConfig.class, 19069);
    kryo.register(CustomCommitAttributes.class, 19070);
    kryo.register(GitCommandType.class, 19071);
    kryo.register(GitCommandResult.class, 19072);
    kryo.register(GitCommandRequest.class, 19073);
    kryo.register(GitCommandStatus.class, 19074);
    kryo.register(AzureVMSSGetVirtualMachineScaleSetParameters.class, 19075);
    kryo.register(AzureVMSSListResourceGroupsNamesParameters.class, 19076);
    kryo.register(AzureVMSSListSubscriptionsParameters.class, 19077);
    kryo.register(AzureVMSSListVirtualMachineScaleSetsParameters.class, 19078);
    kryo.register(AzureVMSSTaskParameters.class, 19079);
    kryo.register(AzureVMSSGetVirtualMachineScaleSetResponse.class, 19080);
    kryo.register(AzureVMSSListResourceGroupsNamesResponse.class, 19081);
    kryo.register(AzureVMSSListSubscriptionsResponse.class, 19082);
    kryo.register(AzureVMSSListVirtualMachineScaleSetsResponse.class, 19083);
    kryo.register(AzureVMSSTaskExecutionResponse.class, 19084);
    kryo.register(AzureVMSSTaskResponse.class, 19085);
    kryo.register(AzureVMSSTaskType.class, 19086);
    kryo.register(YamlGitConfigDTO.class, 19087);
    kryo.register(GitFileChange.class, 19088);
    kryo.register(GitCommitAndPushRequest.class, 19089);
    kryo.register(GitCommitAndPushResult.class, 19090);
    kryo.register(EntityScope.class, 19091);
    kryo.register(GitPushResult.class, 19092);
    kryo.register(GitCommitResult.class, 19093);
    kryo.register(GitFileChange.ChangeType.class, 19094);
    kryo.register(YamlGitConfigDTO.RootFolder.class, 19095);
    kryo.register(EntityScope.Scope.class, 19096);
    kryo.register(GitPushResult.RefUpdate.class, 19097);
  }
}
