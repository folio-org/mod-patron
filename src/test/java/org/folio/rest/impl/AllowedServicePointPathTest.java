package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.patron.utils.Utils.readMockFile;
import static org.folio.rest.impl.PatronResourceImplTest.verifyAllowedServicePoints;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.folio.HttpStatus;
import org.junit.jupiter.api.AfterEach;
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
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class AllowedServicePointPathTest extends BaseResourceServiceTest {
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
    "/circulation-bff/requests/allowed-service-points";
  private static final String SERVICE_POINTS_CIRCULATION_RESPONSE_JSON =
    "/allowed_service_points_circulation_response.json";
  private static final String ACCOUNT_ID_PATH_PARAM_KEY = "accountId";
  private static final String INSTANCE_ID_PATH_PARAM_KEY = "instanceId";

  private static final String INTERNAL_SERVER_ERROR_STATUS_HEADER_VALUE = "status 500";
  private static final String NOT_FOUND_STATUS_HEADER_VALUE = "status 404";
  private static final String INTERNAL_SERVER_ERROR_STATUS_LINE =
    "HTTP/1.1 500 Internal Server Error";
  private static final String ECS_TLR_RESPONSE_WITH_ERROR_EXPECTED =
    "org.folio.patron.rest.exceptions.UnexpectedFetchingException: " +
      "java.util.concurrent.CompletionException: " +
      "io.vertx.core.impl.NoStackTraceTimeoutException: The timeout period of 5000ms has been " +
      "exceeded while executing GET /tlr/settings for server";
  private static final String CIRCULATION_STORAGE_RESPONSE_WITH_ERROR_EXPECTED =
    "io.vertx.core.impl.NoStackTraceTimeoutException: The timeout period of 5000ms has been " +
      "exceeded while executing GET /circulation-settings-storage/circulation-settings for server";
  public static final String EMPTY_STUB_RESPONSE = "null";

  @BeforeEach
  void setUp(Vertx vertx, VertxTestContext context) {
    final String host = "localhost";
    final HttpServer server = vertx.createHttpServer();
    server.requestHandler(this::mockData);
    server.listen(serverPort, host)
      .onSuccess(s -> context.completeNow())
      .onFailure(context::failNow);
  }

  @AfterEach
  void tearDown(VertxTestContext context) {
    context.completeNow();
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
      .pathParam(INSTANCE_ID_PATH_PARAM_KEY, GOOD_INSTANCE_ID)
      .log().all()
      .when()
      .get(accountPath + instancePath + ALLOWED_SERVICE_POINTS_PATH)
      .then()
      .extract()
      .asString();

    final JsonObject expectedJson = new JsonObject(readMockFile(MOCK_DATA_FOLDER +
      expectedServicePointsResponseFileName));

    verifyAllowedServicePoints(expectedJson, new JsonObject(response));
  }

  @Test
  void shouldThrowRuntimeExceptionIfBaseExceptionIsHttpException() {
    String responseWithErrorActual = given()
      .header(new Header(ECS_TLR_HEADER_NAME, INTERNAL_SERVER_ERROR_STATUS_HEADER_VALUE))
      .header(tenantHeader)
      .header(urlHeader)
      .header(contentTypeHeader)
      .pathParam(ACCOUNT_ID_PATH_PARAM_KEY, goodUserId)
      .pathParam(INSTANCE_ID_PATH_PARAM_KEY, GOOD_INSTANCE_ID)
      .log().all()
      .when()
      .get(accountPath + instancePath + ALLOWED_SERVICE_POINTS_PATH)
      .then()
      .and().assertThat().statusCode(HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt())
      .and().assertThat().statusLine(INTERNAL_SERVER_ERROR_STATUS_LINE)
      .extract()
      .asString();

    // Use substring match since the error message includes dynamic server port
    assertTrue(responseWithErrorActual.contains(CIRCULATION_STORAGE_RESPONSE_WITH_ERROR_EXPECTED),
      "Expected error message to contain: " + CIRCULATION_STORAGE_RESPONSE_WITH_ERROR_EXPECTED +
      " but was: " + responseWithErrorActual);
  }

  @Test
  void shouldThrowUnexpectedFetchingExceptionIsNotHttpException() {
    String responseWithErrorActual = given()
      .header(new Header(ECS_TLR_HEADER_NAME, NOT_FOUND_STATUS_HEADER_VALUE))
      .header(tenantHeader)
      .header(urlHeader)
      .header(contentTypeHeader)
      .pathParam(ACCOUNT_ID_PATH_PARAM_KEY, goodUserId)
      .pathParam(INSTANCE_ID_PATH_PARAM_KEY, GOOD_INSTANCE_ID)
      .log().all()
      .when()
      .get(accountPath + instancePath + ALLOWED_SERVICE_POINTS_PATH)
      .then()
      .extract()
      .asString();

    // Use substring match since the error message includes dynamic server port
    assertTrue(responseWithErrorActual.contains(ECS_TLR_RESPONSE_WITH_ERROR_EXPECTED),
      "Expected error message to contain: " + ECS_TLR_RESPONSE_WITH_ERROR_EXPECTED +
      " but was: " + responseWithErrorActual);
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
      String tlrHeader = req.getHeader(ECS_TLR_HEADER_NAME);
      if (INTERNAL_SERVER_ERROR_STATUS_HEADER_VALUE.equals(tlrHeader)) {
        req.response()
          .setStatusCode(HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt())
          .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
          .end(EMPTY_STUB_RESPONSE);
      } else if (NOT_FOUND_STATUS_HEADER_VALUE.equals(tlrHeader)) {
        req.response()
          .setStatusCode(HttpStatus.HTTP_NOT_FOUND.toInt())
          .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER);
      } else if (tlrHeader == null || tlrHeader.isEmpty()) {
        req.response()
          .setStatusCode(HttpStatus.HTTP_OK.toInt())
          .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
          .end(readMockFile(null));
      } else if (ECS_TLR_ENABLED_HEADER.equals(tlrHeader)) {
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
      String circulationHeader = req.getHeader(CIRCULATION_STORAGE_HEADER_NAME);
      String tlrHeader = req.getHeader(ECS_TLR_HEADER_NAME);

      // If TLR request failed with 500, also fail this request to propagate the error
      if (INTERNAL_SERVER_ERROR_STATUS_HEADER_VALUE.equals(tlrHeader)) {
        // Don't respond - let it timeout to simulate error scenario
        // This will cause the overall request to fail with 500
        return;
      }

      if (CIRCULATION_STORAGE_ENABLED_HEADER.equals(circulationHeader)) {
        req.response()
          .setStatusCode(HttpStatus.HTTP_OK.toInt())
          .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
          .end(readMockFile(MOCK_DATA_FOLDER +
            CIRCULATION_STORAGE_MOD_ENABLED_JSON_FILE_PATH));
      } else if (circulationHeader == null || circulationHeader.isEmpty()) {
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
    } else {
      // Default 404 for unknown endpoints
      req.response()
        .setStatusCode(404)
        .putHeader(CONTENT_TYPE_HEADER_NAME, "text/plain")
        .end("Not Found: " + req.path());
    }
  }
}
