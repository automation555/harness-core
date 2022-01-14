package io.harness.yaml.core.variables;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.common.NGExpressionUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.validator.NGRegexValidatorConstants;
import io.harness.validator.NGVariableName;
import io.harness.visitor.helpers.variables.NumberVariableVisitorHelper;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(NGVariableConstants.NUMBER_TYPE)
@SimpleVisitorHelper(helperClass = NumberVariableVisitorHelper.class)
@TypeAlias("io.harness.yaml.core.variables.NumberNGVariable")
@OwnedBy(CDC)
public class NumberNGVariable implements NGVariable {
  @NGVariableName @Pattern(regexp = NGRegexValidatorConstants.IDENTIFIER_PATTERN) String name;
  @ApiModelProperty(allowableValues = NGVariableConstants.NUMBER_TYPE) NGVariableType type = NGVariableType.NUMBER;
  @NotNull
  @YamlSchemaTypes({string})
  @ApiModelProperty(dataType = SwaggerConstants.DOUBLE_CLASSPATH)
  ParameterField<Double> value;
  String description;
  boolean required;
  @JsonProperty("default") Double defaultValue;
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public ParameterField<?> getCurrentValue() {
    return ParameterField.isNull(value)
            || (value.isExpression() && NGExpressionUtils.matchesInputSetPattern(value.getExpressionValue()))
        ? ParameterField.createValueField(defaultValue)
        : value;
  }
}
