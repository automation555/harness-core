package io.harness.serializer.recaster;

import io.harness.beans.CastedField;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.transformers.RecastTransformer;
import io.harness.transformers.simplevalue.CustomValueTransformer;
import io.harness.utils.RecastReflectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@Slf4j
public class JsonObjectRecastTransformer extends RecastTransformer implements CustomValueTransformer {
  private final ObjectMapper objectMapper;
  public JsonObjectRecastTransformer() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
  }

  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    try {
      if (fromObject == null) {
        return NullNode.getInstance();
      }

      Object decodedObject = RecastOrchestrationUtils.getEncodedValue((Document) fromObject);
      if (decodedObject == null) {
        return NullNode.getInstance();
      }

      if (targetClass.isAssignableFrom(ShortNode.class)
          && (decodedObject.getClass().isAssignableFrom(Short.class)
              || decodedObject.getClass().isAssignableFrom(short.class))) {
        return ShortNode.valueOf((Short) decodedObject);
      }
      return objectMapper.valueToTree(decodedObject);
    } catch (Exception e) {
      log.error("Exception while decoding JsonNode {}", fromObject, e);
      throw e;
    }
  }

  /**
   * Here <code>value</code> could be of type JsonObject or JsonArray, so we need to convert it to object
   * <br>
   * After conversion:
   * <br>
   * &emsp;JsonObject -> LinkedHashMap
   * <br>
   * &emsp;JsonArray  -> ArrayList
   */
  @Override
  public Object encode(Object value, CastedField castedField) {
    try {
      return objectMapper.convertValue(value, Object.class);
    } catch (Exception e) {
      log.error("Exception while encoding JsonNode {}", value, e);
      throw e;
    }
  }

  @Override
  public boolean isSupported(Class<?> c, CastedField cf) {
    return RecastReflectionUtils.implementsInterface(c, JsonNode.class);
  }
}
