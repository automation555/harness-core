package software.wings.service;

import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static io.harness.rule.OwnerRule.SAHIL;
import static io.harness.shell.AccessType.USER_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.RUNTIME_PATH;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import io.harness.category.element.UnitTests;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.infrastructure.Host;
import software.wings.service.impl.ServiceCommandExecutorServiceImpl;
import software.wings.service.impl.SshCommandUnitExecutorServiceImpl;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.WingsTestConstants;

import java.util.Map;

/**
 * Created by anubhaw on 6/7/16.
 */
public class ServiceCommandExecutorServiceTest extends WingsBaseTest {
  @Mock private Map<String, CommandUnitExecutorService> commandUnitExecutorServiceMap;
  @Mock private SshCommandUnitExecutorServiceImpl sshCommandUnitExecutorService;
  @Mock private CommandUnitExecutorService commandUnitExecutorService;
  @Mock private EncryptionService encryptionService;
  @InjectMocks private ServiceCommandExecutorService cmdExecutorService = new ServiceCommandExecutorServiceImpl();

  private SettingAttribute hostConnAttrPwd =
      aSettingAttribute().withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build()).build();
  private ExecutionCredential credential =
      aSSHExecutionCredential().withSshUser(USER_NAME).withSshPassword(WingsTestConstants.USER_PASSWORD).build();
  private Host host =
      aHost().withAppId(APP_ID).withHostName(HOST_NAME).withHostConnAttr(hostConnAttrPwd.getUuid()).build();
  private AbstractCommandUnit commandUnit =
      anExecCommandUnit().withName(COMMAND_UNIT_NAME).withCommandString("rm -f $HOME/jetty").build();
  private Command command = aCommand().withName(COMMAND_NAME).addCommandUnits(commandUnit).build();

  @Before
  public void setup() {
    when(commandUnitExecutorServiceMap.get(DeploymentType.KUBERNETES.getDisplayName()))
        .thenReturn(commandUnitExecutorService);
    when(commandUnitExecutorService.execute(any(), any())).thenReturn(SUCCESS);
  }

  /**
   * Should execute command for service instance.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldExecuteCommandForServiceInstance() {
    CommandExecutionContext context = CommandExecutionContext.Builder.aCommandExecutionContext()
                                          .appId(APP_ID)
                                          .activityId(ACTIVITY_ID)
                                          .runtimePath(RUNTIME_PATH)
                                          .executionCredential(credential)
                                          .serviceTemplateId(TEMPLATE_ID)
                                          .host(host)
                                          .deploymentType(DeploymentType.SSH.name())
                                          .executeOnDelegate(true)
                                          .build();
    when(commandUnitExecutorServiceMap.get(DeploymentType.SSH.name())).thenReturn(sshCommandUnitExecutorService);
    when(sshCommandUnitExecutorService.execute(any(AbstractCommandUnit.class), eq(context))).thenReturn(SUCCESS);
    CommandExecutionStatus commandExecutionStatus = cmdExecutorService.execute(command, context);
    assertThat(commandExecutionStatus).isEqualTo(SUCCESS);
  }

  /**
   * Should execute nested command for service instance.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldExecuteNestedCommandForServiceInstance() {
    CommandExecutionContext context = CommandExecutionContext.Builder.aCommandExecutionContext()
                                          .appId(APP_ID)
                                          .activityId(ACTIVITY_ID)
                                          .runtimePath(RUNTIME_PATH)
                                          .executionCredential(credential)
                                          .serviceTemplateId(TEMPLATE_ID)
                                          .host(host)
                                          .deploymentType(DeploymentType.SSH.name())
                                          .executeOnDelegate(true)
                                          .build();
    Command nestedCommand = aCommand().withName("NESTED_CMD").addCommandUnits(command).build();
    when(commandUnitExecutorServiceMap.get(DeploymentType.SSH.name())).thenReturn(sshCommandUnitExecutorService);
    when(sshCommandUnitExecutorService.execute(any(AbstractCommandUnit.class), eq(context))).thenReturn(SUCCESS);
    CommandExecutionStatus commandExecutionStatus = cmdExecutorService.execute(nestedCommand, context);
    assertThat(commandExecutionStatus).isEqualTo(SUCCESS);
    verify(commandUnitExecutorServiceMap).get(DeploymentType.SSH.name());
    verify(sshCommandUnitExecutorService, times(3)).execute(any(AbstractCommandUnit.class), eq(context));
  }

  /**
   * test execute with inlineSSH command
   */
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExecuteInlineSSHCommand() {
    CommandExecutionContext context = CommandExecutionContext.Builder.aCommandExecutionContext()
                                          .appId(APP_ID)
                                          .activityId(ACTIVITY_ID)
                                          .runtimePath(RUNTIME_PATH)
                                          .executionCredential(credential)
                                          .serviceTemplateId(TEMPLATE_ID)
                                          .host(host)
                                          .deploymentType(DeploymentType.SSH.name())
                                          .executeOnDelegate(true)
                                          .inlineSshCommand(true)
                                          .build();
    Command nestedCommand = aCommand().withName("NESTED_CMD").addCommandUnits(command).build();
    when(commandUnitExecutorServiceMap.get(DeploymentType.SSH.name())).thenReturn(sshCommandUnitExecutorService);
    when(sshCommandUnitExecutorService.execute(any(AbstractCommandUnit.class), eq(context))).thenReturn(SUCCESS);
    CommandExecutionStatus commandExecutionStatus = cmdExecutorService.execute(nestedCommand, context);
    assertThat(commandExecutionStatus).isEqualTo(SUCCESS);
    verify(commandUnitExecutorServiceMap).get(DeploymentType.SSH.name());
    verify(sshCommandUnitExecutorService, times(3)).execute(any(AbstractCommandUnit.class), eq(context));
  }

  /**
   * test execute Winrm command
   */
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExecuteWinrm() {
    CommandExecutionContext context = CommandExecutionContext.Builder.aCommandExecutionContext()
                                          .appId(APP_ID)
                                          .activityId(ACTIVITY_ID)
                                          .runtimePath(RUNTIME_PATH)
                                          .executionCredential(credential)
                                          .serviceTemplateId(TEMPLATE_ID)
                                          .host(host)
                                          .deploymentType(DeploymentType.WINRM.name())
                                          .executeOnDelegate(true)
                                          .inlineSshCommand(true)
                                          .build();
    Command nestedCommand = aCommand().withName("NESTED_CMD").addCommandUnits(command).build();
    when(commandUnitExecutorServiceMap.get(DeploymentType.WINRM.name())).thenReturn(sshCommandUnitExecutorService);
    when(sshCommandUnitExecutorService.execute(any(AbstractCommandUnit.class), eq(context))).thenReturn(SUCCESS);
    CommandExecutionStatus commandExecutionStatus = cmdExecutorService.execute(nestedCommand, context);
    assertThat(commandExecutionStatus).isEqualTo(SUCCESS);
    verify(commandUnitExecutorServiceMap).get(DeploymentType.WINRM.name());
    verify(sshCommandUnitExecutorService, times(3)).execute(any(AbstractCommandUnit.class), eq(context));
  }

  /**
   * test execute non-ssh command
   */
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExecuteNonSSHCommand() {
    CommandExecutionContext context = CommandExecutionContext.Builder.aCommandExecutionContext()
                                          .appId(APP_ID)
                                          .activityId(ACTIVITY_ID)
                                          .runtimePath(RUNTIME_PATH)
                                          .executionCredential(credential)
                                          .serviceTemplateId(TEMPLATE_ID)
                                          .host(host)
                                          .deploymentType(DeploymentType.ECS.name())
                                          .executeOnDelegate(true)
                                          .inlineSshCommand(true)
                                          .build();
    Command nestedCommand = aCommand().withName("NESTED_CMD").addCommandUnits(command).build();
    when(commandUnitExecutorServiceMap.get(DeploymentType.ECS.name())).thenReturn(sshCommandUnitExecutorService);
    when(sshCommandUnitExecutorService.execute(any(AbstractCommandUnit.class), eq(context))).thenReturn(SUCCESS);
    CommandExecutionStatus commandExecutionStatus = cmdExecutorService.execute(nestedCommand, context);
    assertThat(commandExecutionStatus).isEqualTo(SUCCESS);
    verify(commandUnitExecutorServiceMap).get(DeploymentType.ECS.name());
    verify(sshCommandUnitExecutorService).execute(any(AbstractCommandUnit.class), eq(context));
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testDecryptCredentialsHostConnectionAttribute() {
    CommandExecutionContext context = CommandExecutionContext.Builder.aCommandExecutionContext()
                                          .appId(APP_ID)
                                          .activityId(ACTIVITY_ID)
                                          .runtimePath(RUNTIME_PATH)
                                          .executionCredential(credential)
                                          .serviceTemplateId(TEMPLATE_ID)
                                          .host(host)
                                          .deploymentType(DeploymentType.ECS.name())
                                          .executeOnDelegate(true)
                                          .inlineSshCommand(true)
                                          .bastionConnectionAttributes(new SettingAttribute())
                                          .winRmConnectionAttributes(WinRmConnectionAttributes.builder().build())
                                          .cloudProviderSetting(new SettingAttribute())
                                          .hostConnectionAttributes(new SettingAttribute())
                                          .build();
    Command nestedCommand = aCommand().withName("NESTED_CMD").addCommandUnits(command).build();
    when(commandUnitExecutorServiceMap.get(DeploymentType.ECS.name())).thenReturn(sshCommandUnitExecutorService);
    when(sshCommandUnitExecutorService.execute(any(AbstractCommandUnit.class), eq(context))).thenReturn(SUCCESS);

    CommandExecutionStatus commandExecutionStatus = cmdExecutorService.execute(nestedCommand, context);

    verify(encryptionService, times(4)).decrypt(any(), any(), eq(false));
    verify(commandUnitExecutorServiceMap).get(DeploymentType.ECS.name());
    verify(sshCommandUnitExecutorService).execute(any(AbstractCommandUnit.class), eq(context));
  }
}
