package test.com.blueskiron.typesafe.rest;

import java.nio.charset.Charset;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blueskiron.typesafe.config.rest.TypesafeConfigHttpServerVerticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class TestTypesafeConfigHttpServerVerticle {

  private static final Logger LOG = LoggerFactory.getLogger(TestTypesafeConfigHttpServerVerticle.class);
  private final String  pathToTestConfig = Paths.get("target", "test-classes", "application.conf").toAbsolutePath().toString();
  private final String rootContext = "test-config";
  private final String host = "localhost";
  private final int port = 8123;
  private Vertx vertx;
  
  @Before
  public void init() {
    vertx = Vertx.vertx();
  }

  @Test
  public void test(TestContext context) {
    Async async = context.async();
    JsonObject config = new JsonObject();
    config.put(TypesafeConfigHttpServerVerticle.HOST_CNFK, host);
    config.put(TypesafeConfigHttpServerVerticle.PORT_CNFK, port);
    config.put(TypesafeConfigHttpServerVerticle.ROOT_CTX_CNFK, rootContext);
    config.put(TypesafeConfigHttpServerVerticle.PATH_TO_CONFIG_CNFK, pathToTestConfig);
    DeploymentOptions depOpts = new DeploymentOptions();
    depOpts.setConfig(config);
    vertx.deployVerticle(new TypesafeConfigHttpServerVerticle(), depOpts, whenDone -> {
      if (whenDone.failed()) {
        context.fail(whenDone.cause());
      }
      HttpClient client = vertx.createHttpClient();
      client.get(port, host, "/"+ rootContext + "/my", response -> {
        response.exceptionHandler(error -> context.fail(error));
        response.bodyHandler(buffer -> {
          String payload = buffer.toString(Charset.forName("UTF-8"));
          JsonObject jsonConfig = new JsonObject(payload);
          LOG.info("Received: {}", jsonConfig.encodePrettily());
          context.assertNotNull(jsonConfig.getValue("key"));
          context.assertNotNull(jsonConfig.getValue("value"));
          async.complete();
        });
      }).end();
    });
  }

}
