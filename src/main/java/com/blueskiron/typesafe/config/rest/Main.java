package com.blueskiron.typesafe.config.rest;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.StringUtil;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class Main {
  private static final String DHOST = "host";
  private static final String DPORT = "port";
  private static final String DROOT_CTX = "rootContext";
  private static final String DCONFIG = "config";

  private static final Map<String, String> PARAMS_MAPPING = new HashMap<>();

  static {
    PARAMS_MAPPING.put(DHOST, TypesafeConfigHttpServerVerticle.HOST_CNFK);
    PARAMS_MAPPING.put(DPORT, TypesafeConfigHttpServerVerticle.PORT_CNFK);
    PARAMS_MAPPING.put(DROOT_CTX, TypesafeConfigHttpServerVerticle.ROOT_CTX_CNFK);
    PARAMS_MAPPING.put(DCONFIG, TypesafeConfigHttpServerVerticle.PATH_TO_CONFIG_CNFK);
  }

  private static Object getPropertyOrDefault(String key, Object fallBack) {
    Object value = System.getProperties().get(key);
    if (value == null) {
     System.out.println("No " + key + " parameter provided, using default = " + fallBack);
      value = fallBack;
    }
    return value;
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    String host = (String) getPropertyOrDefault(DHOST, "localhost");
    int port = Integer.parseInt((String) getPropertyOrDefault(DPORT, 8080));
    String rootCtx = (String) getPropertyOrDefault(DROOT_CTX, "config");
    String configPath = (String) getPropertyOrDefault(DCONFIG, null);
    JsonObject serviceConfig = new JsonObject();
    serviceConfig.put(PARAMS_MAPPING.get(DHOST), host);
    serviceConfig.put(PARAMS_MAPPING.get(DPORT), port);
    serviceConfig.put(PARAMS_MAPPING.get(DROOT_CTX), rootCtx);
    serviceConfig.put(PARAMS_MAPPING.get(DCONFIG), configPath);
    System.out.println("Using serviceConfig = " + serviceConfig.encodePrettily());
    DeploymentOptions depOpts = new DeploymentOptions().setConfig(serviceConfig);
    vertx.deployVerticle(new TypesafeConfigHttpServerVerticle(), depOpts);
  }
}
