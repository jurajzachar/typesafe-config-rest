package test.com.blueskiron.typesafe.rest;

import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blueskiron.typesafe.config.rest.ConfigUtils;
import com.typesafe.config.Config;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

@RunWith(JUnit4.class)
public class TestConfigUtils {
  
  private static final Logger LOG = LoggerFactory.getLogger(TestConfigUtils.class);
  private String testConfigPath = Paths.get("target", "test-classes", "application.conf").toAbsolutePath().toString();
  private Config config;
  
  @Before
  public void readTestConfig() throws Exception {
    config= ConfigUtils.loadAppConfig(testConfigPath).orElseThrow(() -> new Exception("failed to read test config from: " + testConfigPath));
  }
  
  @Test
  public void testTypedConfig() {
    JsonObject jsonObject = ConfigUtils.readJsonObjectValue(config, KingsLanding.class.getName()).orElse(new JsonObject());
    Assert.assertNotNull(jsonObject);
    Assert.assertFalse(jsonObject.isEmpty());
    LOG.debug("read from config: {}", Json.encodePrettily(jsonObject));
  }
  
  @Test
  public void testReadTypedValue() {
    KingsLanding typedValue = ConfigUtils.readTypeValue(config, KingsLanding.class);
    Assert.assertNotNull(typedValue);
    LOG.debug("intialized from config: {}", Json.encodePrettily(typedValue));
  }
}
