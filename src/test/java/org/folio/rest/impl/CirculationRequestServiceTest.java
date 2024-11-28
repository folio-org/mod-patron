package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.patron.utils.Utils.readMockFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.folio.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class CirculationRequestServiceTest extends BaseResourceServiceTest {

  private static final String URL_PATH = "/patron/account/{accountId}/instance/{instanceId}/hold";
  private static final String TLR_SETTINGS_HEADER = "x-okapi-tlr-settings-header";
  private static final String TLR_SETTINGS_HEADER_ENABLED = "true";
  private static final String TLR_SETTINGS_HEADER_DISABLED = "false";
  private static final String TLR_CREATE_REQUEST_RESPONSE_PATH =
    "/create_external_request_tlr_response.json";
  private static final String CIRCULATION_CREATE_REQUEST_RESPONSE_PATH =
    "/create_external_request_circulation_response.json";
  private static final String TLR_POST_PATRON_ACCOUNT_RESPONSE_PATH =
    "/post_patron_account_instance_bff.json";
  private static final String CIRCULATION_POST_PATRON_ACCOUNT_RESPONSE_PATH =
    "/post_patron_account_instance_circulation.json";
  private static final String URL_CIRCULATION_CREATE_INSTANCE_REQUEST =
    "/circulation/requests/instances";
  private static final String CIRCULATION_BFF_CREATE_ECS_REQUEST_EXTERNAL =
    "/circulation-bff/create-ecs-request-external";

  @BeforeEach
  void setUp(Vertx vertx, VertxTestContext context) {
    setUpConnectionForTest(vertx, context);
    final Checkpoint mockOkapiStarted = context.checkpoint(1);
    final String host = "localhost";
    final HttpServer server = vertx.createHttpServer();
    server.requestHandler(this::mockData);
    server.listen(serverPort, host, context.succeeding(id -> mockOkapiStarted.flag()));
  }

  @AfterEach
  void tearDown(Vertx vertx, VertxTestContext context) {
    closeConnectionForTest(vertx, context);
  }

  @Override
  protected void mockData(HttpServerRequest req) {
    if (req.path().equals(ECS_TLR_SETTINGS_PATH)) {
      if (req.getHeader(TLR_SETTINGS_HEADER).equals(TLR_SETTINGS_HEADER_ENABLED)) {
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
    } else if (req.path().equals(URL_CIRCULATION_CREATE_INSTANCE_REQUEST)) {
      req.response()
        .setStatusCode(HttpStatus.HTTP_OK.toInt())
        .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
        .end(readMockFile(MOCK_DATA_FOLDER + CIRCULATION_CREATE_REQUEST_RESPONSE_PATH));
    } else if (req.path().equals(CIRCULATION_BFF_CREATE_ECS_REQUEST_EXTERNAL)) {
      req.response()
        .setStatusCode(HttpStatus.HTTP_OK.toInt())
        .putHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_APPLICATION_JSON_HEADER)
        .end(readMockFile(MOCK_DATA_FOLDER + TLR_CREATE_REQUEST_RESPONSE_PATH));
    }
  }

  @ParameterizedTest
  @MethodSource("provideTestData")
  void postPatronAccountInstanceHoldByIdAndInstanceIdTest(String tlrHeaderValue,
    String responseFileName) {

    String requestBody = readMockFile(MOCK_DATA_FOLDER + "/hold_create_external_request.json");
    var response = given()
      .header(new Header(TLR_SETTINGS_HEADER, tlrHeaderValue))
      .header(tenantHeader)
      .header(urlHeader)
      .header(contentTypeHeader)
      .pathParam(ACCOUNT_ID_PATH_PARAM_KEY, goodUserId)
      .pathParam(INSTANCE_ID_PATH_PARAM_KEY, goodInstanceId)
      .body(requestBody)
      .log().all()
      .when()
      .post(URL_PATH)
      .then()
      .extract()
      .asString();

    var expectedJson = new JsonObject(readMockFile(MOCK_DATA_FOLDER + responseFileName));
    assertEquals(expectedJson, new JsonObject(response));
  }

  private static Stream<Arguments> provideTestData() {
    return Stream.of(
      Arguments.of(TLR_SETTINGS_HEADER_ENABLED, TLR_POST_PATRON_ACCOUNT_RESPONSE_PATH),
      Arguments.of(TLR_SETTINGS_HEADER_DISABLED, CIRCULATION_POST_PATRON_ACCOUNT_RESPONSE_PATH)
    );
  }
}
