package com.blueskiron.typesafe.config.rest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * @author Juraj Zachar (juraj.zachar@gmail.com)
 *
 */
public class TypesafeConfigHttpServerVerticle extends AbstractVerticle {

  /* config key constants */
  public static final String HOST_CNFK = "typesafe.config.rest.host";
  public static final String PORT_CNFK = "typesafe.config.rest.port";
  public static final String ROOT_CTX_CNFK = "typesafe.config.rest.rootContext";
  public static final String PATH_TO_CONFIG_CNFK = "typesafe.config.rest.configPath";

  /* commands constants */
  public static final String RELOAD_CMD = "reload";

  private static final Logger LOG = LoggerFactory.getLogger(TypesafeConfigHttpServerVerticle.class);

  // volatile
  private Config config;

  @Override
  public void start(Future<Void> startFuture) {
    JsonObject myConfig = context.config();
    String host = myConfig.getString(HOST_CNFK);
    int port = myConfig.getInteger(PORT_CNFK);
    String rootCtx = myConfig.getString(ROOT_CTX_CNFK);
    String pathToConfig = myConfig.getString(PATH_TO_CONFIG_CNFK);

    // Path must start with /
    if (rootCtx.charAt(0) != '/') {
      rootCtx = "/" + rootCtx;
    }
    if (!hasValidConfig(host, port, rootCtx, pathToConfig, startFuture)) {
      return;
    }
    Router router = Router.router(vertx);
    Router configRepoRouter = Router.router(vertx);
    loadAppConfig(pathToConfig);
    configRepoRouter.routeWithRegex("\\/(.+)").handler(this::handleRoutingContext);
    router.mountSubRouter(rootCtx, configRepoRouter);
    router.route().handler(BodyHandler.create());
    vertx.createHttpServer(new HttpServerOptions().setHost(host).setPort(port)).requestHandler(router::accept).listen();
    startFuture.complete();
  }

  private void handleRoutingContext(RoutingContext routingContext) {
    String relPath = routingContext.request().getParam("param0");
    if (relPath != null && !relPath.isEmpty()) {
      // chop off trailing '/'
      if (relPath.charAt(relPath.length() - 1) == '/') {
        relPath = relPath.substring(0, relPath.length() - 1);
      }
      final String requestArg = relPath.replace("/", ".");
      if (RELOAD_CMD.equals(requestArg)) {
        LOG.info("Reloading...");
        String pathToConfig = context.config().getString(PATH_TO_CONFIG_CNFK);
        loadAppConfig(pathToConfig);
        routingContext.response()
            .end(toJson(requestArg, String.format("Config '%s' reloaded at %s", pathToConfig, LocalDateTime.now()))
                .encodePrettily());
      } else {
        final Object configValue = readAppConfig(requestArg);
        final String payload = String.format("%s=%s", requestArg, configValue);
        LOG.trace("Serving '{}' to client '{}'", payload, routingContext.request().host());
        routingContext.response().end(toJson(requestArg, configValue).encodePrettily());
      }
    }
  }

  private JsonObject toJson(String key, Object value) {
    return new JsonObject().put("key", key).put("value", value);
  }

  private Object readAppConfig(String configPath) {
    Object value = "null";
    if (config == null) {
      return null;
    } else {
      try {
        value = config.getAnyRef(configPath);
      } catch (ConfigException e) {
        // if no config value is found we return null;
      }
    }
    return value;
  }

  private boolean hasValidConfig(String host, int port, String rootCtx, String pathToconfig, Future<Void> startFuture) {
    boolean isValid = true;
    if (host == null) {
      host = "localhost";
    }
    if (port == 0) {
      port = 80;
    }
    if (rootCtx == null) {
      startFuture.fail("Cannot start without '" + ROOT_CTX_CNFK + "' value!");
      isValid = false;
    }
    if (!startFuture.failed() && rootCtx.isEmpty()) {
      startFuture.fail("Cannot start without a valid '" + ROOT_CTX_CNFK + "' value!");
      isValid = false;
    }
    if (!startFuture.failed() && pathToconfig == null) {
      startFuture.fail("Cannot start without '" + ROOT_CTX_CNFK + "' value!");
      isValid = false;
    }
    if (!startFuture.failed() && !Files.exists(Paths.get(pathToconfig))) {
      startFuture.fail("Cannot read Typesafe Config: '" + pathToconfig + "'. File exists?");
      isValid = false;
    }
    return isValid;
  }

  private void loadAppConfig(String pathToConfig) {
    File file = Paths.get(pathToConfig).toFile();
    if (!file.exists()) {
      LOG.error("Cannot read Typesafe Config: '{}'", file);
      config = ConfigFactory.empty();
    } else {
      LOG.info("Loading Typesafe Config: '{}'", pathToConfig);
      config = ConfigFactory.parseFile(file);
    }
  }
}
