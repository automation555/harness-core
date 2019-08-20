package io.harness.data.structure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;

public class MapUtilTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void shouldPutIfNotEmpty() {
    Map<String, String> input = new HashMap<>();
    MapUtils.putIfNotEmpty("key", "value", input);
    assertNotNull(input.get("key"));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotPutIfEmpty() {
    Map<String, String> input = new HashMap<>();
    MapUtils.putIfNotEmpty("key", "", input);
    assertThat(input.get("key")).isNull();
  }
}