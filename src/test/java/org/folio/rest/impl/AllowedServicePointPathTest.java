package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.patron.utils.Utils.readMockFile;
import static org.folio.rest.impl.PatronResourceImplTest.verifyAllowedServicePoints;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.patron.utils.Utils;
import org.folio.rest.RestVerticle;
import org.folio.rest.tools.utils.ModuleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class AllowedServicePointPathTest {
  public static final String CONTENT_TYPE_HEADER_NAME = "content-type";
  private static final String CONTENT_TYPE_APPLICATION_JSON_HEADER = "application/json";
  private static final String CIRCULATION_STORAGE_HEADER_NAME = "x-okapi-bad-user-id";
  private static final String ECS_TLR_HEADER_NAME = "x-okapi-bad-data";
  private static final String EMPTY_HEADER = "";
  private static final String CIRCULATION_STORAGE_ENABLED_HEADER = "circulation-storage-true";
  private static final String CIRCULATION_STORAGE_DISABLED_HEADER = "circulation-storage-false";
  private static final String ECS_TLR_ENABLED_HEADER = "ecs-tlr-true";
  private static final String ECS_TLR_DISABLED_HEADER = "ecs-tlr-false";
  private static final String ECS_TLR_MOD_ENABLED_JSON_FILE_PATH =
    "/ecs_tlr_module_feature_enabled.json";
  private static final String ECS_TLR_MOD_DISABLED_JSON_FILE_PATH =
    "/ecs_tlr_module_feature_disabled.json";
  private static final String ECS_TLR_SETTINGS_PATH = "/tlr/settings";
  private static final String CIRCULATION_STORAGE_MOD_ENABLED_JSON_FILE_PATH =
    "/circulation_storage_module_feature_enabled.json";
  private static final String CIRCULATION_STORAGE_MOD_DISABLED_JSON_FILE_PATH =
    "/circulation_storage_module_feature_disabled.json";
  public static final String ALLOWED_SERVICE_POINTS_CIRCULATION_JSON_FILE_PATH =
    "/allowed_service_points_circulation.json";
  public static final String ALLOWED_SERVICE_POINTS_CIRCULATION_BFF_JSON_FILE_PATH =
    "/allowed_service_points_circulation_bff.json";
  private static final String CIRCULATION_SETTINGS_STORAGE_PATH =
    "/circulation-settings-storage/circulation-settings";
  private static final String SERVICE_POINTS_CIRCULATION_BFF_RESPONSE_JSON =
    "/allowed_service_points_circulation_bff_response.json";
  private static final String CIRCULATION_REQUESTS_ALLOWED_SERVICE_POINTS_PATH =
    "/circulation/requests/allowed-service-points";
  private static final String CIRCULATION_BFF_ALLOWED_SERVICE_POINTS_PATH =
    "/circulation-bff/allowed-service-points";
  private static final String SERVICE_POINTS_CIRCULATION_RESPONSE_JSON =
    "/allowed_service_points_circulation_response.json";
  private static final String ACCOUNT_ID_PATH_PARAM_KEY = "accountId";
  private static final String INSTANCE_ID_PATH_PARAM_KEY = "instanceId";
  private final Logger logger = LogManager.getLogger();
  private final int okapiPort = Utils.getRandomPort();
  private final int serverPort = Utils.getRandomPort();

  private final Header contentTypeHeader =
    new Header("Content-Type", "application/json");
  private final String mockDataFolder = "PatronServicesResourceImpl";
  private final Header tenantHeader = new Header("X-Okapi-Tenant",
    "patronresourceimpltest");
  private final Header urlHeader = new Header("X-Okapi-Url", "http://localhost:" +
    serverPort);
  private final String goodUserId = "1ec54964-70f0-44cc-bd19-2a892ea0d336";
  private final String goodInstanceId = "f39fd3ca-e3fb-4cd9-8cf9-48e7e2c494e5";
  private final String accountPath = "/patron/account/{accountId}";
  private final String instancePath = "/instance/{instanceId}";
  private final String allowedServicePointsPath = "/allowed-service-points";

  private String moduleName;
  private String moduleVersion;
  private String moduleId;

  @BeforeEach
  public void setUp(Vertx vertx, VertxTestContext context) {
    vertx.exceptionHandler(context::failNow);

    moduleName = ModuleName.getModuleName().replaceAll("_", "-");
    moduleVersion = ModuleName.getModuleVersion();
    moduleId = moduleName + "-" + moduleVersion;
    logger.info("Test setup starting for {}", moduleId);

    final JsonObject conf = new JsonObject();
    conf.put("http.port", okapiPort);

    final Checkpoint verticleStarted = context.checkpoint(1);
    final Checkpoint mockOkapiStarted = context.checkpoint(1);

    final DeploymentOptions opt = new DeploymentOptions().setConfig(conf);
    vertx.deployVerticle(RestVerticle.class.getName(), opt,
      context.succeeding(id -> verticleStarted.flag()));
    RestAssured.port = okapiPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    logger.info("Patron Services Test Setup Done using port {}", okapiPort);

    final String host = "localhost";

    final HttpServer server = vertx.createHttpServer();
    server.requestHandler(req -> {
      if (req.path().equals(CIRCULATION_REQUESTS_ALLOWED_SERVICE_POINTS_PATH)) {
        req.response()
          .setStatusCode(HttpStatus.HTTP_OK.toInt())
          .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
          .end(readMockFile(mockDataFolder +
            ALLOWED_SERVICE_POINTS_CIRCULATION_JSON_FILE_PATH));
      } else if (req.path().equals(CIRCULATION_BFF_ALLOWED_SERVICE_POINTS_PATH)) {
        req.response()
          .setStatusCode(HttpStatus.HTTP_OK.toInt())
          .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
          .end(readMockFile(mockDataFolder +
            ALLOWED_SERVICE_POINTS_CIRCULATION_BFF_JSON_FILE_PATH));
      } else if (req.path().equals(ECS_TLR_SETTINGS_PATH)) {
        if (req.getHeader(ECS_TLR_HEADER_NAME).isEmpty()) {
          req.response()
            .setStatusCode(HttpStatus.HTTP_OK.toInt())
            .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
            .end(readMockFile(null));
        } else if (req.getHeader(ECS_TLR_HEADER_NAME).equals(ECS_TLR_ENABLED_HEADER)) {
          req.response()
            .setStatusCode(HttpStatus.HTTP_OK.toInt())
            .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
            .end(readMockFile(mockDataFolder + ECS_TLR_MOD_ENABLED_JSON_FILE_PATH));
        } else {
          req.response()
            .setStatusCode(HttpStatus.HTTP_OK.toInt())
            .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
            .end(readMockFile(mockDataFolder + ECS_TLR_MOD_DISABLED_JSON_FILE_PATH));
        }
      } else if (req.path().equals(CIRCULATION_SETTINGS_STORAGE_PATH)) {
        if (req.getHeader(CIRCULATION_STORAGE_HEADER_NAME)
          .equals(CIRCULATION_STORAGE_ENABLED_HEADER)) {
          req.response()
            .setStatusCode(HttpStatus.HTTP_OK.toInt())
            .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
            .end(readMockFile(mockDataFolder +
              CIRCULATION_STORAGE_MOD_ENABLED_JSON_FILE_PATH));
        } else if (req.getHeader(CIRCULATION_STORAGE_HEADER_NAME).isEmpty()) {
          req.response()
            .setStatusCode(HttpStatus.HTTP_OK.toInt())
            .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
            .end(readMockFile(null));
        } else {
          req.response()
            .setStatusCode(HttpStatus.HTTP_OK.toInt())
            .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
            .end(readMockFile(mockDataFolder +
              CIRCULATION_STORAGE_MOD_DISABLED_JSON_FILE_PATH));
        }
      }
    });
    server.listen(serverPort, host, context.succeeding(id -> mockOkapiStarted.flag()));
  }

  @AfterEach
  public void tearDown(Vertx vertx, VertxTestContext context) {
    logger.info("Patron Services Testing Complete");
    vertx.close(ar -> {
      if (ar.succeeded()) {
        context.completeNow();
      } else {
        context.failNow(ar.cause());
      }
    });
  }

  @ParameterizedTest
  @MethodSource("headerValueToFileName")
  void getAllowedServicePointsShouldUseCorrectRoutingPathTest(String tlrHeaderValue,
    String circulationStoragesHeaderValue, String expectedServicePointsResponseFileName) {

    var response = given()
      .header(new Header(ECS_TLR_HEADER_NAME, tlrHeaderValue))
      .header(new Header(CIRCULATION_STORAGE_HEADER_NAME, circulationStoragesHeaderValue))
      .header(tenantHeader)
      .header(urlHeader)
      .header(contentTypeHeader)
      .pathParam(ACCOUNT_ID_PATH_PARAM_KEY, goodUserId)
      .pathParam(INSTANCE_ID_PATH_PARAM_KEY, goodInstanceId)
      .log().all()
      .when()
      .get(accountPath + instancePath + allowedServicePointsPath)
      .then()
      .extract()
      .asString();

    final JsonObject expectedJson = new JsonObject(readMockFile(mockDataFolder +
      expectedServicePointsResponseFileName));

    verifyAllowedServicePoints(expectedJson, new JsonObject(response));
  }

  private static Stream<Arguments> headerValueToFileName() {
    return Stream.of(
      Arguments.of(
        ECS_TLR_ENABLED_HEADER, EMPTY_HEADER,
        SERVICE_POINTS_CIRCULATION_BFF_RESPONSE_JSON
      ),
      Arguments.of(
        ECS_TLR_DISABLED_HEADER, EMPTY_HEADER,
        SERVICE_POINTS_CIRCULATION_RESPONSE_JSON
      ),
      Arguments.of(
        EMPTY_HEADER, CIRCULATION_STORAGE_ENABLED_HEADER,
        SERVICE_POINTS_CIRCULATION_BFF_RESPONSE_JSON
      ),
      Arguments.of(
        EMPTY_HEADER, CIRCULATION_STORAGE_DISABLED_HEADER,
        SERVICE_POINTS_CIRCULATION_RESPONSE_JSON
      ),
      Arguments.of(
        EMPTY_HEADER, EMPTY_HEADER, SERVICE_POINTS_CIRCULATION_RESPONSE_JSON
      )
    );
  }
}
