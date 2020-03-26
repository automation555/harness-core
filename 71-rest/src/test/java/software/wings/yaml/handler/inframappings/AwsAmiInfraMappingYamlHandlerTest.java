package software.wings.yaml.handler.inframappings;

import static io.harness.rule.OwnerRule.GEORGE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsAmiInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.inframapping.AwsAmiInfraMappingYamlHandler;
import software.wings.service.intfc.InfrastructureMappingService;

public class AwsAmiInfraMappingYamlHandlerTest extends BaseInfraMappingYamlHandlerTest {
  private String validYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: AWS_AMI\n"
      + "amiDeploymentType: AWS_ASG\n"
      + "autoScalingGroupName: Abaris__AMI__Test__deploy__v2__QA__Env__1\n"
      + "classicLoadBalancers:\n"
      + "- todolist-lb\n"
      + "computeProviderName: aws\n"
      + "computeProviderType: AWS\n"
      + "deploymentType: AMI\n"
      + "infraMappingType: AWS_AMI\n"
      + "region: us-east-1\n"
      + "serviceName: SERVICE_NAME";

  @InjectMocks @Inject private AwsAmiInfraMappingYamlHandler yamlHandler;

  @InjectMocks @Inject private InfrastructureMappingService infrastructureMappingService;

  private String validYamlFilePath =
      "Setup/Applications/APP_NAME/Environments/ENV_NAME/Service Infrastructure/aws-ami.yaml";
  private String infraMappingName = "aws-ami";

  @Before
  public void runBeforeTest() {
    setup(validYamlFilePath, infraMappingName);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws Exception {
    ChangeContext<Yaml> changeContext = getChangeContext(validYamlContent, validYamlFilePath, yamlHandler);

    Yaml yamlObject = (Yaml) getYaml(validYamlContent, Yaml.class);
    changeContext.setYaml(yamlObject);

    AwsAmiInfrastructureMapping infrastructureMapping =
        yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertThat(infrastructureMapping).isNotNull();
    assertThat(infraMappingName).isEqualTo(infrastructureMapping.getName());

    Yaml yaml = yamlHandler.toYaml(infrastructureMapping, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isEqualTo(InfrastructureMappingType.AWS_AMI.name());

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertThat(yamlContent).isEqualTo(validYamlContent);

    InfrastructureMapping infrastructureMapping2 =
        infrastructureMappingService.getInfraMappingByName(APP_ID, ENV_ID, infraMappingName);
    // TODO find out why this couldn't be called
    //    Workflow savedWorkflow = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertThat(infrastructureMapping2).isNotNull().hasFieldOrPropertyWithValue("name", infraMappingName);

    yamlHandler.delete(changeContext);

    AwsAmiInfrastructureMapping afterDelete = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertThat(afterDelete).isNull();
  }
}
