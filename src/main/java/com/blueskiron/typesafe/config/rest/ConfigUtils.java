package com.blueskiron.typesafe.config.rest;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;

import io.vertx.core.VertxOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ConfigUtils {
  
  private static final Logger LOG = LoggerFactory.getLogger(ConfigUtils.class);
  private static final ConfigRenderOptions CONFIG_RENDER_OPTS = ConfigRenderOptions.concise();
  
  /**
   * @param pathToConfig absolute path to {@link Config}
   * @return
   */
  public static Optional<Config> loadAppConfig(String pathToConfig) {
    File file = Paths.get(pathToConfig).toFile();
    Optional<Config> maybeConfig = Optional.empty();
    if (!file.exists()) {
      LOG.error("Cannot read Typesafe Config: '" + file + "' File exists?");
    } else {
      LOG.info("Loading Typesafe Config: '{}'", pathToConfig);
      try {
        Config config = ConfigFactory.parseFile(file).resolve();
        maybeConfig = Optional.of(config);
      } catch (Exception e) {
        LOG.error("Failed to read Typesafe Config: ", e);
      }
    }
    return maybeConfig;
  }
  
  /**
   * @param config Config
   * @return
   */
  public static VertxOptions readVertxOptions(Config config) {
    VertxOptions opts = new VertxOptions();
    JsonObject jsonObject = readJsonObjectValue(config, VertxOptions.class.getName()).orElse(new JsonObject());
    if(jsonObject.isEmpty()) {
      LOG.warn("Failed to read Vert.X options from provided configuration, falling back to defaults...");
    } else {
      opts = new VertxOptions(jsonObject);
    }
    return opts;
  }
  
  
  /**
   * @param config
   * @param type
   * @return
   * @throws IllegalArgumentException
   */
  public static <T> T readTypeValue(Config config, Class<T> type) throws IllegalArgumentException {
    String key= type.getClass().getName();
    try {
      String raw = config.getValue(key).render(CONFIG_RENDER_OPTS);
      return Json.decodeValue(raw, type);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to read configration for '" + key + "'! " + e.getMessage());
    }
  }
  
  /**
   * @param config
   * @param type
   * @param deserializer
   * @return
   * @throws IllegalArgumentException
   */
  public static <T> T readTypeValue(Config config, Class<T> type, BiFunction<String,Class<T>,T> deserializer) throws IllegalArgumentException {
    String key= type.getClass().getName();
    try {
      String raw = config.getValue(key).render(CONFIG_RENDER_OPTS);
      return deserializer.apply(raw, type);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to read configration for '" + key + "'! " + e.getMessage());
    }
  }
  
  /**
   * @param config
   * @param key
   * @return
   */
  public static Optional<JsonObject> readJsonObjectValue(Config config, String key) {
    Optional<JsonObject> maybeConfigValue = Optional.empty();
    try {
      ConfigValue value = config.getValue(key);
      String raw = value.render(CONFIG_RENDER_OPTS);
      JsonObject jsonObj = new JsonObject(raw);
      maybeConfigValue = Optional.of(jsonObj);
    } catch (Exception e) {
      //ignore
      LOG.error("Failed to read value for key '{}'", key, e);
    }
    return maybeConfigValue;
  }
  
  /**
   * @param config
   * @param key
   * @return
   */
  public static Optional<JsonArray> readJsonArrayValue(Config config, String key) {
    Optional<JsonArray> maybeConfigValue = Optional.empty();
    try {
      ConfigValue value = config.getValue(key);
      String raw = value.render(CONFIG_RENDER_OPTS);
      JsonArray jsonArray = new JsonArray(raw);
      maybeConfigValue = Optional.of(jsonArray);
    } catch (Exception e) {
      //ignore
      LOG.error("Failed to read value for key '{}'", key, e);
    }
    return maybeConfigValue;
  }
  
  /**
   * @param config
   * @param key
   * @param fallbackValue
   * @return
   */
  public static Object getValueOrDefault(Config config, String key, Object fallbackValue) {
    if(config.hasPath(key)) {
      return config.getAnyRef(key);
    } else {
      LOG.warn("Requested config path: '{}', using default value: '{}'", key, fallbackValue);
      return fallbackValue;
    }
  }
      
}
