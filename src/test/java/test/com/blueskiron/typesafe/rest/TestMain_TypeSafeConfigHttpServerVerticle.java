package test.com.blueskiron.typesafe.rest;

import java.nio.file.Paths;

import com.blueskiron.typesafe.config.rest.TypesafeConfigHttpServerVerticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class TestMain_TypeSafeConfigHttpServerVerticle {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    String pwd = Paths.get("").toAbsolutePath().toString();
    JsonObject config = new JsonObject();
    config.put(TypesafeConfigHttpServerVerticle.HOST_CNFK, "localhost");
    config.put(TypesafeConfigHttpServerVerticle.PORT_CNFK, 8123);
    config.put(TypesafeConfigHttpServerVerticle.ROOT_CTX_CNFK, "test-config");
    config.put(TypesafeConfigHttpServerVerticle.PATH_TO_CONFIG_CNFK, pwd + "/src/test/resources/application.conf");
    DeploymentOptions depOpts = new DeploymentOptions();
    depOpts.setConfig(config);
    vertx.deployVerticle(new TypesafeConfigHttpServerVerticle(), depOpts);
  }

}
