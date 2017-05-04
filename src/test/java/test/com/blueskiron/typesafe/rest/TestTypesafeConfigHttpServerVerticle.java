package test.com.blueskiron.typesafe.rest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.blueskiron.typesafe.config.rest.TypesafeConfigHttpServerVerticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class TestTypesafeConfigHttpServerVerticle {

  private static final String TEST_ROOT_CTX = "test-config";
  private Vertx vertx;

  @Before
  private void init() {
    vertx = Vertx.vertx();
  }

  @Test
  public void test(TestContext context) {

    Async async = context.async();
    JsonObject config = new JsonObject();
    config.put(TypesafeConfigHttpServerVerticle.HOST_CNFK, "localhost");
    config.put(TypesafeConfigHttpServerVerticle.PORT_CNFK, 8123);
    config.put(TypesafeConfigHttpServerVerticle.ROOT_CTX_CNFK, TEST_ROOT_CTX);
    DeploymentOptions depOpts = new DeploymentOptions();
    depOpts.setConfig(config);
    vertx.deployVerticle(new TypesafeConfigHttpServerVerticle(), depOpts);

  }

}
