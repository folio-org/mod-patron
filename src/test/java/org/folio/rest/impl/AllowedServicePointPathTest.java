package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.patron.utils.Utils.readMockFile;
import static org.folio.rest.impl.PatronResourceImplTest.verifyAllowedServicePoints;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.folio.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.restassured.http.Header;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class AllowedServicePointPathTest extends BaseResourceServiceTest{
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

  private static final String INTERNAL_SERVER_ERROR_STATUS_HEADER_VALUE = "status 500";
  private static final String INTERNAL_SERVER_ERROR_STATUS_LINE = "HTTP/1.1 500 Internal Server Error";
  private static final String RESPONSE_WITH_ERROR_EXPECTED = "java.lang.RuntimeException: " +
    "java.util.concurrent.CompletionException: io.vertx.core.impl.NoStackTraceTimeoutException: " +
    "The timeout period of 5000ms has been exceeded while executing GET /tlr/settings " +
    "for server null";

  @BeforeEach
  @Override
  public void setUp(Vertx vertx, VertxTestContext context) {
    super.setUp(vertx, context);
    final Checkpoint mockOkapiStarted = context.checkpoint(1);
    final String host = "localhost";
    final HttpServer server = vertx.createHttpServer();
    server.requestHandler(this::mockData);
    server.listen(serverPort, host, context.succeeding(id -> mockOkapiStarted.flag()));
  }

  @Override
  public void tearDown(Vertx vertx, VertxTestContext context) {
    super.tearDown(vertx, context);
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

    final JsonObject expectedJson = new JsonObject(readMockFile(MOCK_DATA_FOLDER +
      expectedServicePointsResponseFileName));

    verifyAllowedServicePoints(expectedJson, new JsonObject(response));
  }

  @Test
  void shouldThrowRuntimeExceptionIfBaseExceptionIsNotHttpException() {
    String responseWithErrorActual  =  given()
      .header(new Header(ECS_TLR_HEADER_NAME, INTERNAL_SERVER_ERROR_STATUS_HEADER_VALUE))
      .header(tenantHeader)
      .header(urlHeader)
      .header(contentTypeHeader)
      .pathParam(ACCOUNT_ID_PATH_PARAM_KEY, goodUserId)
      .pathParam(INSTANCE_ID_PATH_PARAM_KEY, goodInstanceId)
      .log().all()
      .when()
      .get(accountPath + instancePath + allowedServicePointsPath)
      .then()
      .and().assertThat().statusCode(HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt())
      .and().assertThat().statusLine(INTERNAL_SERVER_ERROR_STATUS_LINE)
      .extract()
      .asString();

    assertEquals(RESPONSE_WITH_ERROR_EXPECTED, responseWithErrorActual);
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


  public void mockData(HttpServerRequest req) {
    if (req.path().equals(CIRCULATION_REQUESTS_ALLOWED_SERVICE_POINTS_PATH)) {
      req.response()
        .setStatusCode(HttpStatus.HTTP_OK.toInt())
        .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
        .end(readMockFile(MOCK_DATA_FOLDER +
          ALLOWED_SERVICE_POINTS_CIRCULATION_JSON_FILE_PATH));
    } else if (req.path().equals(CIRCULATION_BFF_ALLOWED_SERVICE_POINTS_PATH)) {
      req.response()
        .setStatusCode(HttpStatus.HTTP_OK.toInt())
        .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
        .end(readMockFile(MOCK_DATA_FOLDER +
          ALLOWED_SERVICE_POINTS_CIRCULATION_BFF_JSON_FILE_PATH));
    } else if (req.path().equals(ECS_TLR_SETTINGS_PATH)) {
      if(req.getHeader(ECS_TLR_HEADER_NAME).equals(INTERNAL_SERVER_ERROR_STATUS_HEADER_VALUE)) {
        req.response()
          .setStatusCode(HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt())
          .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER);
      } else if (req.getHeader(ECS_TLR_HEADER_NAME).isEmpty()) {
        req.response()
          .setStatusCode(HttpStatus.HTTP_OK.toInt())
          .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
          .end(readMockFile(null));
      } else if (req.getHeader(ECS_TLR_HEADER_NAME).equals(ECS_TLR_ENABLED_HEADER)) {
        req.response()
          .setStatusCode(HttpStatus.HTTP_OK.toInt())
          .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
          .end(readMockFile(MOCK_DATA_FOLDER + ECS_TLR_MOD_ENABLED_JSON_FILE_PATH));
      } else {
        req.response()
          .setStatusCode(HttpStatus.HTTP_OK.toInt())
          .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
          .end(readMockFile(MOCK_DATA_FOLDER + ECS_TLR_MOD_DISABLED_JSON_FILE_PATH));
      }
    } else if (req.path().equals(CIRCULATION_SETTINGS_STORAGE_PATH)) {
      if (req.getHeader(CIRCULATION_STORAGE_HEADER_NAME)
        .equals(CIRCULATION_STORAGE_ENABLED_HEADER)) {
        req.response()
          .setStatusCode(HttpStatus.HTTP_OK.toInt())
          .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
          .end(readMockFile(MOCK_DATA_FOLDER +
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
          .end(readMockFile(MOCK_DATA_FOLDER +
            CIRCULATION_STORAGE_MOD_DISABLED_JSON_FILE_PATH));
      }
    }
  }
}
