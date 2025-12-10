package org.folio.rest.impl;

import io.restassured.specification.RequestSpecification;
import io.vertx.core.Future;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.patron.utils.Utils;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.persist.PostgresClient;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.junit5.VertxTestContext;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.support.OkapiHeaders;
import org.folio.support.OkapiUrl;
import org.folio.support.VertxModule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseResourceServiceTest {
  private static final Logger logger = LogManager.getLogger();
  private static final int okapiPort = NetworkUtils.nextFreePort();
  private static final String OKAPI_TENANT = "patronresourceimpltest";

  protected static final String MOCK_DATA_FOLDER = "PatronServicesResourceImpl";
  protected final int serverPort = Utils.getRandomPort();
  protected final Header urlHeader = new Header("X-Okapi-Url", "http://localhost:" +
    serverPort);
  protected final Header contentTypeHeader =
    new Header("Content-Type", "application/json");
  protected final Header tenantHeader = new Header("X-Okapi-Tenant","patronresourceimpltest");
  protected final String goodUserId = "1ec54964-70f0-44cc-bd19-2a892ea0d336";
  protected static final String GOOD_INSTANCE_ID = "f39fd3ca-e3fb-4cd9-8cf9-48e7e2c494e5";
  protected final String accountPath = "/patron/account/{accountId}";
  protected final String instancePath = "/instance/{instanceId}";
  protected static final String ALLOWED_SERVICE_POINTS_PATH = "/allowed-service-points";
  protected static final String ALLOWED_SERVICE_POINTS_MULTI_ITEM_PATH = "/allowed-service-points-multi-item";
  protected static final String BATCH_REQUEST_PATH = "/batch-request";
  protected static final String BATCH_REQUEST_STATUS_PATH = "/batch-request/{batchId}/status";
  protected static final String BATCH_REQUEST_ID = "5203c035-005e-4a70-b555-ddaa3094c51c";
  protected static final String COMPLETED_BATCH_REQUEST_ID = "5203c035-005e-4a70-b555-ddaa3094c51f";
  protected static final String FAILED_BATCH_REQUEST_ID = "5203c035-005e-4a70-b555-ddaa3094c51g";
  protected static final String NON_EXISTING_BATCH_REQUEST_ID = "5203c035-005e-4a70-b555-ddaa3094c51d";
  protected static final String INVALID_BATCH_REQUEST_ID = "5203c035-005e-4a70-b555-ddaa3094c51e";

  protected static PostgresClient postgresClient;
  protected static OkapiUrl okapiUrl;
  protected static OkapiHeaders okapiHeaders;
  protected static VertxModule module;

  protected abstract void mockData(HttpServerRequest req);

  @BeforeAll
  @SneakyThrows
  static void setUpClass(Vertx vertx, VertxTestContext context) {
    PostgresClient.setPostgresTester(new PostgresTesterContainer());
    okapiUrl = new OkapiUrl("http://localhost:" + okapiPort);
    okapiHeaders = new OkapiHeaders(okapiUrl, OKAPI_TENANT, null);
    module = new VertxModule(vertx);

    module.deployModule(okapiPort)
      .compose(res -> module.enableModule(okapiHeaders, false, false))
      .onComplete(result -> {
        if (result.failed()) {
          context.failNow(result.cause());
          return;
        }
        postgresClient = PostgresClient.getInstance(vertx, OKAPI_TENANT);
        RestAssured.port = okapiPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        context.completeNow();
      });
    logger.info("Patron Services Test Setup Done using port {}", okapiPort);
  }

  @AfterAll
  public static void tearDownClass(VertxTestContext context) {
    logger.info("Patron Services Testing Complete");
    module.purgeModule(okapiHeaders)
      .compose(v -> {
        PostgresClient.stopPostgresTester();
        return Future.succeededFuture();
      })
      .onComplete(context.succeedingThenComplete());
  }

  protected RequestSpecification getRequestSpecification() {
    return RestAssured.given()
      .header(contentTypeHeader)
      .header(tenantHeader)
      .header(urlHeader)
      .when();
  }
}
