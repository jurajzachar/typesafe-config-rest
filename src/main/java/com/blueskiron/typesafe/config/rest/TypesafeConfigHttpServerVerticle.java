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
import com.typesafe.config.ConfigRenderOptions;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
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
  private Future<Config> configFuture = Future.future();

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
    Route route = configRepoRouter.route(HttpMethod.POST, "/reload");
    route.handler(this::handleReload);
    //return the whole encoded config when fetching root
    configRepoRouter.get("/").handler(ctx -> {
      if (configFuture.failed() || !configFuture.isComplete()) {
        ctx.response().setStatusCode(500).end(Json.encodePrettily(configFuture.cause()));
      } else {
        ctx.response().setStatusCode(200).end(configFuture.result().root().render(ConfigRenderOptions.concise()));
      }
    });
    //do dynamic route translation when fetching parts of the config
    configRepoRouter.routeWithRegex("\\/(.+)").handler(this::handleRoutingContext);
    router.mountSubRouter(rootCtx, configRepoRouter);
    router.route().handler(BodyHandler.create());
    vertx.createHttpServer(new HttpServerOptions().setHost(host).setPort(port)).requestHandler(router::accept).listen();
    startFuture.complete();
  }

  private void handleReload(RoutingContext routingContext) {
    LOG.info("Reloading...");
    String pathToConfig = context.config().getString(PATH_TO_CONFIG_CNFK);
    loadAppConfig(pathToConfig);
    if(configFuture.failed()){
      routingContext.response().setStatusCode(500)
      .end(String.format("Failed to reload Config '%s': %s", pathToConfig, configFuture.cause()));
    } else {
    routingContext.response().headers().add("Access-Control-Allow-Origin", "*");
    routingContext.response().end(
        toJson("reload", String.format("Config '%s' reloaded sucessfully at %s", pathToConfig, LocalDateTime.now()))
            .encodePrettily());
    }
  }

  private void handleRoutingContext(RoutingContext routingContext) {
    String relPath = routingContext.request().getParam("param0");
    if (relPath != null && !relPath.isEmpty()) {
      // chop off trailing '/'
      if (relPath.charAt(relPath.length() - 1) == '/') {
        relPath = relPath.substring(0, relPath.length() - 1);
      }
      final String requestArg = relPath.replace("/", ".");
      final Object configValue = readAppConfig(requestArg);
      final String payload = String.format("%s=%s", requestArg, configValue);
      LOG.trace("Serving '{}' to client '{}'", payload, routingContext.request().host());
      handleCORS(routingContext.response())
      .end(toJson(requestArg, configValue).encodePrettily());
    }
  }
  
  private HttpServerResponse handleCORS(HttpServerResponse response){
    response.headers()
    .add("Content-Type", "application/json")
    .add("Access-Control-Allow-Origin", "*");
    return response;
  }
  
  private JsonObject toJson(String key, Object value) {
    return new JsonObject().put("key", key).put("value", value);
  }

  private Object readAppConfig(String configPath) {
    Object value = "null";
    if (configFuture.failed() || !configFuture.isComplete()) {
      return null;
    } else {
      try {
        value = configFuture.result().getAnyRef(configPath);
      } catch (ConfigException e) {
        // if no config value is found we return null;
        value = e.getMessage();
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
      rootCtx = "/";
    }
    if (!startFuture.failed() && pathToconfig == null) {
      startFuture.fail("Cannot start without '" + PATH_TO_CONFIG_CNFK + "' value!");
      isValid = false;
    }
    if (!startFuture.failed() && !Files.exists(Paths.get(pathToconfig))) {
      startFuture.fail("Cannot read Typesafe Config: '" + pathToconfig + "'. File exists?");
      isValid = false;
    }
    return isValid;
  }

  private void loadAppConfig(String pathToConfig) {
    if(configFuture.isComplete()){
      configFuture = Future.future();
    }
    File file = Paths.get(pathToConfig).toFile();
    if (!file.exists()) {
      String errorMsg = "Cannot read Typesafe Config: '" + file + "' File exists?";
      LOG.error(errorMsg);
      configFuture.fail(errorMsg);
    } else {
      LOG.info("Loading Typesafe Config: '{}'", pathToConfig);
      try {
        Config config = ConfigFactory.parseFile(file).resolve();
        configFuture.complete(config);
      } catch (Exception e) {
        configFuture.fail("Failed to read Typesafe Config: " + e);
      }
    }
  }
}
