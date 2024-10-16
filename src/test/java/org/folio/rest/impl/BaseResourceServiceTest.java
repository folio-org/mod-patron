package org.folio.rest.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.patron.utils.Utils;
import org.folio.rest.RestVerticle;
import org.folio.rest.tools.utils.ModuleName;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;

public abstract class BaseResourceServiceTest {
  protected static final String MOCK_DATA_FOLDER = "PatronServicesResourceImpl";
  protected final int serverPort = Utils.getRandomPort();
  private final int okapiPort = Utils.getRandomPort();
  protected final Header urlHeader = new Header("X-Okapi-Url", "http://localhost:" +
    serverPort);
  protected final Header contentTypeHeader =
    new Header("Content-Type", "application/json");
  protected final Header tenantHeader = new Header("X-Okapi-Tenant",
    "patronresourceimpltest");
  protected final String goodUserId = "1ec54964-70f0-44cc-bd19-2a892ea0d336";
  protected final String goodInstanceId = "f39fd3ca-e3fb-4cd9-8cf9-48e7e2c494e5";
  protected final String accountPath = "/patron/account/{accountId}";
  protected final String instancePath = "/instance/{instanceId}";
  protected final String allowedServicePointsPath = "/allowed-service-points";
  private final Logger logger = LogManager.getLogger();

  protected abstract void mockData(HttpServerRequest req);

  protected void setUpConnectionForTest(Vertx vertx, VertxTestContext context) {
    vertx.exceptionHandler(context::failNow);

    String moduleName = ModuleName.getModuleName().replaceAll("_", "-");
    String moduleVersion = ModuleName.getModuleVersion();
    String moduleId = moduleName + "-" + moduleVersion;

    logger.info("Test setup starting for {}", moduleId);

    final JsonObject conf = new JsonObject();
    conf.put("http.port", okapiPort);

    final Checkpoint verticleStarted = context.checkpoint(1);

    final DeploymentOptions opt = new DeploymentOptions().setConfig(conf);
    vertx.deployVerticle(RestVerticle.class.getName(), opt,
      context.succeeding(id -> verticleStarted.flag()));
    RestAssured.port = okapiPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    logger.info("Patron Services Test Setup Done using port {}", okapiPort);
  }

  protected void closeConnectionForTest(Vertx vertx, VertxTestContext context) {
    logger.info("Patron Services Testing Complete");
    vertx.close(ar -> {
      if (ar.succeeded()) {
        context.completeNow();
      } else {
        context.failNow(ar.cause());
      }
    });
  }
}
