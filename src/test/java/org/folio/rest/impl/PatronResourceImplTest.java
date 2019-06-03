package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.ContentType.TEXT;
import static org.folio.patron.utils.Utils.readMockFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.patron.utils.Utils;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Hold;
import org.folio.rest.tools.PomReader;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class PatronResourceImplTest {
  private final Logger logger = LogManager.getLogger();

  private String moduleName;
  private String moduleVersion;
  private String moduleId;

  private final int okapiPort = Utils.getRandomPort();
  private final int serverPort = Utils.getRandomPort();

  private final Header tenantHeader = new Header("X-Okapi-Tenant", "patronresourceimpltest");
  private final Header urlHeader = new Header("X-Okapi-Url", "http://localhost:" + serverPort);
  private final Header contentTypeHeader = new Header("Content-Type", "application/json");

  private final String mockDataFolder = "PatronServicesResourceImpl";
  private final String accountPath = "/patron/account/{accountId}";
  private final String itemPath = "/item/{itemId}";
  private final String instancePath = "/instance/{instanceId}";
  private final String holdPath = "/hold";
  private final String holdIdPath = "/{holdId}";
  private final String renewPath = "/renew";

  private final String goodUserId = "1ec54964-70f0-44cc-bd19-2a892ea0d336";
  private final String inactiveUserId = "4a87f60c-ebb1-4726-a9b2-548cdd17bbd4";
  private final String badUserId = "3ed07e77-a5c9-47c8-bb0b-381099e10a42";
  private final String goodItemId = "32e5757d-6566-466e-b69d-994eb33d2b62";
  private final String badItemId = "3dda4eb9-a156-474c-829f-bd5a386f382c";
  private final String goodInstanceId = "f39fd3ca-e3fb-4cd9-8cf9-48e7e2c494e5";
  private final String goodHoldId = "dd238b5b-01fc-4205-83b8-ce27a650d827";
  private final String badHoldId = "1745628c-f424-4b50-a116-e18be37cd599";

  private final String chargeItemBook1Id = "e785f572-c5d4-4bbc-91ba-c0d62ebebc20";
  private final String chargeItemBook2Id = "cb958743-ddcd-4bf6-907a-e6962b66bfe9";
  private final String chargeItemBook3Id = "95546593-f846-4df2-8f34-9bf5debbcd10";
  private final String chargeItemCameraId = "5531b437-349c-4453-9361-69082324949f";
  private final String itemBook1Id = "7d9dfe70-0158-489d-a7ed-2789eac277b3";
  private final String itemBook2Id = "7d4bfd9c-dc46-46a1-89bd-160c61fe46d8";
  private final String itemBook3Id = "688be386-5522-4505-ad8e-60d84385d43f";
  private final String itemCameraId = "240e521c-12df-4744-a5ab-313862ec1752";
  private final String holdingsBook1Id = "ace20b0f-1b35-41ae-8ce2-2c7cc9a98819";
  private final String holdingsBook2Id = "75d0799a-66d8-46cf-a7e3-ed7390425112";
  private final String holdingsBook3Id = "39a2de0a-95a3-4870-9320-57476afc2faf";
  private final String holdingsCameraId = "29c35636-08d2-46d8-bb37-c1209a0db638";
  private final String instanceBook1Id = "6e024cd5-c19a-4fe0-a2cd-64ce5814c694";
  private final String instanceBook2Id = "b3f5ef6d-2309-4935-858d-870cd7801632";
  private final String instanceBook3Id = "f3482bed-a7e9-4f07-beb0-ebd693331350";
  private final String instanceCameraId = "c394b514-9fd0-496d-ab9a-aec777facc1b";
  private final String book1Barcode = "1234567890";
  private final String book2Barcode = "1234567891";
  private final String book3Barcode = "1234567892";
  private final String cameraBarcode = "1234567893";
  private final String feeFineOverdueId = "cdf3970f-7ed2-4dae-8ae3-a8250a83a9a0";
  private final String feeFineDamageBookId = "881c628b-e1c4-4711-b9d7-090af40f6a8f";
  private final String feeFineDamageEquipmentId = "ca295e87-223f-403c-9eee-a152c47bf67f";

  static {
    System.setProperty("vertx.logger-delegate-factory-class-name",
        "io.vertx.core.logging.Log4j2LogDelegateFactory");
  }

  @BeforeEach
  public void setUp(Vertx vertx, VertxTestContext context) throws Exception {
    moduleName = PomReader.INSTANCE.getModuleName().replaceAll("_", "-");
    moduleVersion = PomReader.INSTANCE.getVersion();
    moduleId = moduleName + "-" + moduleVersion;
    logger.info("Test setup starting for " + moduleId);

    final JsonObject conf = new JsonObject();
    conf.put("http.port", okapiPort);

    final Checkpoint verticleStarted = context.checkpoint(1);
    final Checkpoint mockOkapiStarted = context.checkpoint(1);

    final DeploymentOptions opt = new DeploymentOptions().setConfig(conf);
    vertx.deployVerticle(RestVerticle.class.getName(), opt,
        context.succeeding(id -> verticleStarted.flag()));
    RestAssured.port = okapiPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    logger.info("Patron Services Test Setup Done using port " + okapiPort);

    final String host = "localhost";

    final HttpServer server = vertx.createHttpServer();
    server.requestHandler(req -> {
      if (req.path().equals(String.format("/users/%s", goodUserId))) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/user_active.json"));
      } else if (req.path().equals(String.format("/users/%s", inactiveUserId))) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/user_not_active.json"));
      } else if (req.path().equals(String.format("/users/%s", badUserId))) {
        req.response()
          .setStatusCode(404)
          .putHeader("content-type", "text/plain")
          .end("Not Found");
      } else if (req.path().equals("/circulation/loans")) {
        if (req.query().equals(String.format("limit=%d&query=%%28userId%%3D%%3D%s%%20and%%20status.name%%3D%%3DOpen%%29", Integer.MAX_VALUE, goodUserId))) {
          req.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/loans_all.json"));
        } else if (req.query().equals(String.format("limit=%d&query=%%28userId%%3D%%3D%s%%20and%%20status.name%%3D%%3DOpen%%29", 1, goodUserId))) {
          req.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/loans_totals.json"));
        } else {
          req.response().setStatusCode(500).end("Unexpected call: " + req.path());
        }
      } else if (req.path().equals("/circulation/requests")) {
        if (req.method() == HttpMethod.POST) {
          final String badDataValue = req.getHeader("x-okapi-bad-data");
          if (badDataValue != null) {
            if (badDataValue.equals("java.lang.NullPointerException")) {
              req.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json")
                .end("{}");
            } else {
              req.response()
                .setStatusCode(Integer.parseInt(badDataValue))
                .putHeader("content-type", "text/plain")
                .end(badDataValue);
            }
          } else {
            req.response()
              .setStatusCode(201)
              .putHeader("content-type", "application/json")
              .end(readMockFile(mockDataFolder + "/holds_create.json"));
          }
        } else {
          if (req.query().equals(String.format("limit=%d&query=%%28requesterId%%3D%%3D%s%%20and%%20status%%3D%%3DOpen%%2A%%29", Integer.MAX_VALUE, goodUserId))) {
            req.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(readMockFile(mockDataFolder + "/holds_all.json"));
          } else if (req.query().equals(String.format("limit=%d&query=%%28requesterId%%3D%%3D%s%%20and%%20status%%3D%%3DOpen%%2A%%29", 1, goodUserId))) {
            req.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(readMockFile(mockDataFolder + "/holds_totals.json"));
          } else {
            req.response().setStatusCode(500).end("Unexpected call: " + req.path());
          }
        }
      } else if (req.path().equals("/circulation/requests/instances")) {
        if (req.method() == HttpMethod.POST) {
          final String badDataValue = req.getHeader("x-okapi-bad-data");
          if (badDataValue != null) {
            if (badDataValue.equals("java.lang.NullPointerException")) {
              req.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json")
                .end("{}");
            } else {
              req.response()
                .setStatusCode(Integer.parseInt(badDataValue))
                .putHeader("content-type", "text/plain")
                .end(badDataValue);
            }
          } else {
            req.response()
              .setStatusCode(201)
              .putHeader("content-type", "application/json")
              .end(readMockFile(mockDataFolder + "/instance_holds_create.json"));
          }
        } else {
          req.response().setStatusCode(500).end("Unexpected call: " + req.path());
        }
      } else if (req.path().equals("/circulation/requests/" + goodHoldId)) {
        if (req.method() == HttpMethod.DELETE) {
          final String badDataValue = req.getHeader("x-okapi-bad-data");
          if (badDataValue != null) {
            if (badDataValue.equals("java.lang.NullPointerException")) {
              req.response()
                .setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end("java.lang.NullPointerException");
            } else {
              req.response()
                .setStatusCode(Integer.parseInt(badDataValue))
                .putHeader("content-type", "text/plain")
                .end(badDataValue);
            }
          } else {
            req.response()
              .setStatusCode(204)
              .end();
          }
        } else {
          req.response().setStatusCode(500).end("Unexpected call: " + req.path());
        }
      } else if (req.path().equals("/circulation/requests/" + badHoldId)) {
        if (req.method() == HttpMethod.DELETE) {
          req.response()
            .setStatusCode(404)
            .end();
        } else {
          req.response().setStatusCode(500).end("Unexpected call: " + req.path());
        }
      } else if (req.path().equals("/accounts")) {
        if (req.query().equals(String.format("limit=%d&query=%%28userId%%3D%%3D%s%%20and%%20status.name%%3D%%3DOpen%%29", Integer.MAX_VALUE, goodUserId))) {
          req.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/accounts_all.json"));
        } else {
          req.response().setStatusCode(500).end("Unexpected call: " + req.path());
        }
      } else if (req.path().equals("/chargeitem/" + chargeItemBook1Id)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/chargeitem_book1.json"));
      } else if (req.path().equals("/chargeitem/" + chargeItemBook2Id)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/chargeitem_book2.json"));
      } else if (req.path().equals("/chargeitem/" + chargeItemBook3Id)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/chargeitem_book3.json"));
      } else if (req.path().equals("/chargeitem/" + chargeItemCameraId)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/chargeitem_camera.json"));
      } else if (req.path().equals("/holdings-storage/holdings/" + holdingsBook1Id)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/holdings_book1.json"));
      } else if (req.path().equals("/holdings-storage/holdings/" + holdingsBook2Id)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/holdings_book2.json"));
      } else if (req.path().equals("/holdings-storage/holdings/" + holdingsBook3Id)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/holdings_book3.json"));
      } else if (req.path().equals("/holdings-storage/holdings/" + holdingsCameraId)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/holdings_camera.json"));
      } else if (req.path().equals("/inventory/instances/" + instanceBook1Id)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/instance_book1.json"));
      } else if (req.path().equals("/inventory/instances/" + instanceBook2Id)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/instance_book2.json"));
      } else if (req.path().equals("/inventory/instances/" + instanceBook3Id)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/instance_book3.json"));
      } else if (req.path().equals("/inventory/instances/" + instanceCameraId)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/instance_camera.json"));
      } else if (req.path().equals("/inventory/items/" + itemBook1Id)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/item_book1.json"));
      } else if (req.path().equals("/inventory/items/" + itemBook2Id)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/item_book2.json"));
      } else if (req.path().equals("/inventory/items/" + itemBook3Id)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/item_book3.json"));
      } else if (req.path().equals("/inventory/items/" + itemCameraId)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/item_camera.json"));
      } else if (req.path().equals("/inventory/items/")) {
        if (req.query().equals(String.format("query=barcode%%3D%%3D%s", book1Barcode))) {
          req.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/item_book1.json"));
        } else if (req.query().equals(String.format("query=barcode%%3D%%3D%s", book2Barcode))) {
          req.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/item_book2.json"));
        } else if (req.query().equals(String.format("query=barcode%%3D%%3D%s", book3Barcode))) {
          req.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/item_book3.json"));
        } else if (req.query().equals(String.format("query=barcode%%3D%%3D%s", cameraBarcode))) {
          req.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/item_camera.json"));
        } else {
          req.response().setStatusCode(500).end("Unexpected call: " + req.path());
        }
      } else if (req.path().equals("/feefines/" + feeFineOverdueId)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/feefine_overdue.json"));
      } else if (req.path().equals("/feefines/" + feeFineDamageBookId)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/feefine_damage_book.json"));
      } else if (req.path().equals("/feefines/" + feeFineDamageEquipmentId)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/feefine_damage_equipment.json"));
      } else if (req.path().equals("/circulation/renew-by-id")) {
        if (req.getHeader("x-okapi-bad-user-id") != null) {
          req.response()
            .setStatusCode(422)
            .putHeader("content-type", "application/json")
            .sendFile(mockDataFolder + "/renew_bad_user_id.json");
        } else if (req.getHeader("x-okapi-bad-item-id") != null) {
          req.response()
            .setStatusCode(422)
            .putHeader("content-type", "application/json")
            .sendFile(mockDataFolder + "/renew_bad_item_id.json");
        } else if (req.getHeader("x-okapi-bad-data") != null) {
          final String badDataValue = req.getHeader("x-okapi-bad-data");
          if (badDataValue.equals("java.lang.NullPointerException")) {
            req.response()
              .setStatusCode(201)
              .putHeader("content-type", "application/json")
              .end("{}");
          } else if (badDataValue.equals("422")) {
            req.response()
              .setStatusCode(422)
              .putHeader("content-type", "application/json")
              .end("{\"errors\":[{\"message\":\"error\"}]}");
          } else {
            req.response()
              .setStatusCode(Integer.parseInt(badDataValue))
              .putHeader("content-type", "text/plain")
              .end(badDataValue);
          }
        } else {
          req.response()
            .setStatusCode(201)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/renew_create.json"));
        }
      } else {
        req.response().setStatusCode(500).end("Unexpected call: " + req.path());
      }
    });

    server.listen(serverPort, host, context.succeeding(id -> mockOkapiStarted.flag()));
  }

  @AfterEach
  public void tearDown(Vertx vertx, VertxTestContext context) {
    logger.info("Patron Services Testing Complete");
    vertx.close(context.completing());
  }

  @Test
  public final void testGetPatronAccountById() {
    logger.info("Testing for successful patron services account retrieval by id");

    final Response r = given()
        .log().all()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", goodUserId)
        .queryParam("includeLoans", "true")
        .queryParam("includeHolds", "true")
        .queryParam("includeCharges", "true")
      .when()
        .get(accountPath)
      .then()
          .log().all()
          .contentType(ContentType.JSON)
          .statusCode(200)
          .extract().response();

    final String body = r.getBody().asString();
    final JsonObject json = new JsonObject(body);
    final JsonObject expectedJson = new JsonObject(readMockFile(mockDataFolder + "/response_testGetPatronAccountById.json"));

    assertEquals(3, json.getInteger("totalLoans"));
    assertEquals(3, json.getJsonArray("loans").size());

    assertEquals(3, json.getInteger("totalHolds"));
    assertEquals(3, json.getJsonArray("holds").size());

    JsonObject money = json.getJsonObject("totalCharges");
    assertEquals(155.0, money.getDouble("amount"));
    assertEquals("USD", money.getString("isoCurrencyCode"));
    assertEquals(4, json.getInteger("totalChargesCount"));
    assertEquals(4, json.getJsonArray("charges").size());

    for (int i = 0; i < 4; i++) {
      final JsonObject jo = json.getJsonArray("charges").getJsonObject(i);

      boolean found = false;
      for (int j = 0; j < 4; j++) {
        final JsonObject expectedJO = expectedJson.getJsonArray("charges").getJsonObject(j);
        if (verifyCharge(expectedJO, jo)) {
          found = true;
          break;
        }
      }

      if (found == false) {
        fail("Unexpected charge: " + jo.toString());
      }
    }

    for (int i = 0; i < 3; i++) {
      final JsonObject jo = json.getJsonArray("holds").getJsonObject(i);

      boolean found = false;
      for (int j = 0; j < 3; j++) {
        final JsonObject expectedJO = expectedJson.getJsonArray("holds").getJsonObject(j);
        if (verifyHold(expectedJO, jo)) {
          found = true;
          break;
        }
      }

      if (found == false) {
        fail("Unexpected id: " + jo.getString("requestId"));
      }
    }

    for (int i = 0; i < 3; i++) {
      final JsonObject jo = json.getJsonArray("loans").getJsonObject(i);

      boolean found = false;
      for (int j = 0; j < 3; j++) {
        final JsonObject expectedJO = expectedJson.getJsonArray("loans").getJsonObject(j);
        if (verifyLoan(expectedJO, jo)) {
          found = true;
          break;
        }
      }

      if (found == false) {
        fail("Unexpected loan: " + jo.toString());
      }
    }

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testGetPatronAccountByIdNoLists() {
    logger.info("Testing for successful patron services account retrieval by id without item lists");

    final Response r = given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", goodUserId)
      .when()
        .get(accountPath)
      .then()
        .log().all()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .extract().response();

    final String body = r.getBody().asString();
    final JsonObject json = new JsonObject(body);

    assertEquals(3, json.getInteger("totalLoans"));
    assertEquals(0, json.getJsonArray("loans").size());

    assertEquals(3, json.getInteger("totalHolds"));
    assertEquals(0, json.getJsonArray("holds").size());

    final JsonObject money = json.getJsonObject("totalCharges");
    assertEquals(155.0, money.getDouble("amount"));
    assertEquals("USD", money.getString("isoCurrencyCode"));
    assertEquals(4, json.getInteger("totalChargesCount"));
    assertEquals(0, json.getJsonArray("charges").size());

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testGetPatronAccountById400UserNotActive() {
    logger.info("Testing for 400 due to patron account not active");

    given()
      .header(tenantHeader)
      .header(urlHeader)
      .header(contentTypeHeader)
      .pathParam("accountId", inactiveUserId)
    .when()
      .get(accountPath)
    .then()
      .log().all()
      .contentType(ContentType.TEXT)
      .statusCode(400);

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testGetPatronAccountById404() {
    logger.info("Testing for 404 due to unknown user id");

    given()
      .header(tenantHeader)
      .header(urlHeader)
      .header(contentTypeHeader)
      .pathParam("accountId", badUserId)
    .when()
      .get(accountPath)
    .then()
      .log().all()
      .contentType(ContentType.TEXT)
      .statusCode(404);

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testPostPatronAccountByIdItemByItemIdRenew() {
    logger.info("Testing renew for 201");

    final Response r = given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", goodUserId)
        .pathParam("itemId", goodItemId)
      .when()
        .post(accountPath + itemPath + renewPath)
      .then()
        .log().all()
        .contentType(ContentType.JSON)
        .statusCode(201)
        .extract().response();

    final String body = r.getBody().asString();
    final JsonObject json = new JsonObject(body);
    final JsonObject expectedJson = new JsonObject(readMockFile(mockDataFolder + "/response_testPostPatronAccountByIdItemByItemIdRenew.json"));

    verifyLoan(expectedJson, json);

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testPostPatronAccountByIdItemByItemIdRenew422BadUserId() {
    logger.info("Testing renew for 422 due to a bad user id");

    final Response r = given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .header(new Header("x-okapi-bad-user-id", badUserId))
        .pathParam("accountId", badUserId)
        .pathParam("itemId", goodItemId)
      .when()
        .post(accountPath + itemPath + renewPath)
      .then()
        .log().all()
        .contentType(ContentType.JSON)
        .statusCode(422)
        .extract().response();

    final String body = r.getBody().asString();
    final Errors errors = Json.decodeValue(body, Errors.class);

    assertNotNull(errors);
    assertNotNull(errors.getErrors());
    assertEquals(1, errors.getErrors().size());
    assertEquals("Cannot renew item checked out to different user", errors.getErrors().get(0).getMessage());
    assertNotNull(errors.getErrors().get(0).getParameters());
    assertEquals(1, errors.getErrors().get(0).getParameters().size());
    assertEquals("userId", errors.getErrors().get(0).getParameters().get(0).getKey());
    assertEquals(badUserId, errors.getErrors().get(0).getParameters().get(0).getValue());

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testPostPatronAccountByIdItemByItemIdRenew422BadItemId() {
    logger.info("Testing renew for 422 due to a bad item id");

    final Response r = given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .header(new Header("x-okapi-bad-item-id", badItemId))
        .pathParam("accountId", goodUserId)
        .pathParam("itemId", badItemId)
      .when()
        .post(accountPath + itemPath + renewPath)
      .then()
        .log().all()
        .contentType(ContentType.JSON)
        .statusCode(422)
        .extract().response();

    final String body = r.getBody().asString();
    final Errors errors = Json.decodeValue(body, Errors.class);

    assertNotNull(errors);
    assertNotNull(errors.getErrors());
    assertEquals(1, errors.getErrors().size());
    assertEquals("No item with ID " + badItemId + " exists", errors.getErrors().get(0).getMessage());
    assertNotNull(errors.getErrors().get(0).getParameters());
    assertEquals(1, errors.getErrors().get(0).getParameters().size());
    assertEquals("itemId", errors.getErrors().get(0).getParameters().get(0).getKey());
    assertEquals(badItemId, errors.getErrors().get(0).getParameters().get(0).getValue());

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testPostPatronAccountByIdItemByItemIdHold() {
    logger.info("Testing creating a hold on an item for the specified user");

    final Response r = given()
        .log().all()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .body(readMockFile(mockDataFolder + "/request_testPostPatronAccountByIdItemByItemIdHold.json"))
        .pathParam("accountId", goodUserId)
        .pathParam("itemId", goodItemId)
      .when()
        .post(accountPath + itemPath + holdPath)
      .then()
        .log().all()
        .contentType(ContentType.JSON)
        .statusCode(201)
        .extract().response();

    final String body = r.getBody().asString();
    final JsonObject json = new JsonObject(body);
    final JsonObject expectedJson = new JsonObject(readMockFile(mockDataFolder + "/response_testPostPatronAccountByIdItemByItemIdHold.json"));

    verifyHold(expectedJson, json);

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testPutPatronAccountByIdItemByItemIdHoldByHoldId() {
    logger.info("Testing edit hold for 501");

    given()
      .header(tenantHeader)
      .header(urlHeader)
      .header(contentTypeHeader)
      .pathParam("accountId", badUserId)
      .pathParam("itemId", "c9b8958e-dea6-4547-843f-02001d5265ff")
      .pathParam("holdId", "1745628c-f424-4b50-a116-e18be37cd599")
    .when()
      .put(accountPath + itemPath + holdPath + holdIdPath)
    .then()
      .log().all()
      .statusCode(501);

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testDeletePatronAccountByIdItemByItemIdHoldByHoldId() {
    logger.info("Testing delete hold by id");

    given()
      .header(tenantHeader)
      .header(urlHeader)
      .header(contentTypeHeader)
      .pathParam("accountId", goodUserId)
      .pathParam("itemId", goodItemId)
      .pathParam("holdId", goodHoldId)
    .when()
      .delete(accountPath + itemPath + holdPath + holdIdPath)
    .then()
      .log().all()
      .statusCode(204);

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testDeletePatronAccountByIdItemByItemIdHoldByHoldId404() {
    logger.info("Testing delete hold by with an unknown id");

    given()
      .header(tenantHeader)
      .header(urlHeader)
      .header(contentTypeHeader)
      .pathParam("accountId", goodUserId)
      .pathParam("itemId", goodItemId)
      .pathParam("holdId", badHoldId)
    .when()
      .delete(accountPath + itemPath + holdPath + holdIdPath)
    .then()
      .log().all()
      .statusCode(404);

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testPostPatronAccountByIdInstanceByInstanceIdHold() {
    logger.info("Testing creating a hold on an instance for the specified user");

    final Hold hold = given()
        .headers(new Headers(tenantHeader, urlHeader, contentTypeHeader))
        .and().pathParams("accountId", goodUserId, "instanceId", goodInstanceId)
        .and().body(readMockFile(mockDataFolder
            + "/request_testPostPatronAccountByIdInstanceByInstanceIdHold.json"))
      .when()
        .post(accountPath + instancePath + holdPath)
      .then()
        .log().all()
        .and().assertThat().contentType(ContentType.JSON)
        .and().assertThat().statusCode(201)
      .extract()
        .as(Hold.class);

    final Hold expectedHold = Json.decodeValue(readMockFile(mockDataFolder + "/response_testPostPatronAccountByIdInstanceByInstanceIdHold.json"), Hold.class);

    assertEquals(expectedHold, hold);

    // Test done
    logger.info("Test done");
  }

  @ParameterizedTest
  @MethodSource("instanceHoldsFailureCodes")
  public final void testPostPatronAccountByIdInstanceByInstanceIdHoldWithErrors(
      String codeString, int expectedCode) {
    logger.info("Testing creating a hold on an instance for the specified user with a {} error",
        codeString);

    given()
      .headers(new Headers(tenantHeader, urlHeader, contentTypeHeader,
          new Header("x-okapi-bad-data", codeString)))
      .and().pathParams("accountId", goodUserId, "instanceId", goodInstanceId)
      .and().body(readMockFile(mockDataFolder
          + "/request_testPostPatronAccountByIdInstanceByInstanceIdHold.json"))
    .when()
      .post(accountPath + instancePath + holdPath)
    .then()
      .log().all()
      .and().assertThat().statusCode(expectedCode)
      .and().assertThat().contentType(ContentType.TEXT)
      .and().assertThat().body(Matchers.equalTo(codeString));

    // Test done
    logger.info("Test done");
  }

  @ParameterizedTest
  @MethodSource("itemHoldsFailureCodes")
  public final void testPostPatronAccountByIdItemByItemIdHoldWithErrors(
      String codeString, int expectedCode) {
    logger.info("Testing creating a hold on an item for the specified user with a {} error",
        codeString);

    given()
      .headers(new Headers(tenantHeader, urlHeader, contentTypeHeader,
          new Header("x-okapi-bad-data", codeString)))
      .and().pathParams("accountId", goodUserId, "itemId", goodItemId)
      .and().body(readMockFile(mockDataFolder
          + "/request_testPostPatronAccountByIdItemByItemIdHold.json"))
    .when()
      .post(accountPath + itemPath + holdPath)
    .then()
      .log().all()
      .and().assertThat().statusCode(expectedCode)
      .and().assertThat().contentType(ContentType.TEXT)
      .and().assertThat().body(Matchers.equalTo(codeString));

    // Test done
    logger.info("Test done");
  }

  @ParameterizedTest
  @MethodSource("renewFailureCodes")
  public final void testPostPatronAccountByIdItemByItemIdRenewWithErrors(
      String codeString, int expectedCode, String expectedMessage,
      ContentType expectedContentType) {
    logger.info("Testing renew for with a {} error", codeString);

    given()
      .headers(new Headers(tenantHeader, urlHeader, contentTypeHeader,
          new Header("x-okapi-bad-data", codeString)))
      .and().pathParams("accountId", goodUserId, "itemId", goodItemId)
    .when()
      .post(accountPath + itemPath + renewPath)
    .then()
      .log().all()
      .and().assertThat().statusCode(expectedCode)
      .and().assertThat().contentType(expectedContentType)
      .and().assertThat().body(Matchers.equalTo(expectedMessage));

    // Test done
    logger.info("Test done");
  }

  @ParameterizedTest
  @MethodSource("itemHoldsDeleteFailureCodes")
  public final void testDeletePatronAccountByIdItemByItemIdHoldByHoldIdWithErrors(
      String codeString, int expectedCode) {
    logger.info("Testing deleting a hold on an item for the specified user with a {} error",
        codeString);

    given()
      .headers(new Headers(tenantHeader, urlHeader, contentTypeHeader,
          new Header("x-okapi-bad-data", codeString)))
      .and().pathParams("accountId", goodUserId, "itemId", goodItemId, "holdId", goodHoldId)
    .when()
      .delete(accountPath + itemPath + holdPath + holdIdPath)
    .then()
      .log().all()
      .and().assertThat().statusCode(expectedCode)
      .and().assertThat().contentType(ContentType.TEXT)
      .and().assertThat().body(Matchers.equalTo(codeString));

    // Test done
    logger.info("Test done");
  }

  static Stream<Arguments> instanceHoldsFailureCodes() {
    return Stream.of(
        // Even though we receive a 400, we need to return a 500 since there is nothing the client
        // can do to correct the 400. We'd have to correct it in the code.
        Arguments.of("400", 500),
        Arguments.of("401", 401),
        Arguments.of("403", 403),
        Arguments.of("404", 404),
        Arguments.of("500", 500),
        Arguments.of("java.lang.NullPointerException", 500)
      );
  }

  static Stream<Arguments> itemHoldsFailureCodes() {
    return Stream.of(
        // Even though we receive a 400, we need to return a 500 since there is nothing the client
        // can do to correct the 400. We'd have to correct it in the code.
        Arguments.of("400", 500),
        Arguments.of("401", 401),
        Arguments.of("403", 403),
        Arguments.of("404", 404),
        Arguments.of("500", 500),
        Arguments.of("java.lang.NullPointerException", 500)
      );
  }

  static Stream<Arguments> renewFailureCodes() {
    return Stream.of(
        // Even though we receive a 400, we need to return a 500 since there is nothing the client
        // can do to correct the 400. We'd have to correct it in the code.
        Arguments.of("400", 500, "400", TEXT),
        Arguments.of("401", 401, "401", TEXT),
        Arguments.of("403", 403, "403", TEXT),
        Arguments.of("404", 404, "404", TEXT),
        Arguments.of("422", 422, new JsonObject(
            "{\"errors\":[{\"message\":\"error\", \"parameters\":[]}]}") .encodePrettily(), JSON),
        Arguments.of("500", 500, "500", TEXT),
        Arguments.of("java.lang.NullPointerException", 500, "java.lang.NullPointerException", TEXT)
      );
  }

  static Stream<Arguments> itemHoldsDeleteFailureCodes() {
    return Stream.of(
        // Even though we receive a 400, we need to return a 500 since there is nothing the client
        // can do to correct the 400. We'd have to correct it in the code.
        Arguments.of("400", 500),
        Arguments.of("401", 401),
        Arguments.of("403", 403),
        Arguments.of("404", 404),
        Arguments.of("500", 500)
      );
  }

  private boolean verifyCharge(JsonObject expectedCharge, JsonObject actualCharge) {
    // Bad check, but each date is unique in the mock data.
    if (expectedCharge.getString("accrualDate").equals(actualCharge.getString("accrualDate"))) {
      assertEquals(expectedCharge.getString("state"), actualCharge.getString("state"));
      assertEquals(expectedCharge.getString("reason"), actualCharge.getString("reason"));

      verifyAmount(expectedCharge.getJsonObject("chargeAmount"), actualCharge.getJsonObject("chargeAmount"));

      return verifyItem(expectedCharge.getJsonObject("item"), actualCharge.getJsonObject("item"));
    }

    return false;
  }

  private void verifyAmount(JsonObject expectedAmount, JsonObject actualAmount) {
    assertEquals(expectedAmount.getDouble("amount"), actualAmount.getDouble("amount"));
    assertEquals(expectedAmount.getString("isoCurrencyCode"), actualAmount.getString("isoCurrencyCode"));
  }

  private boolean verifyHold(JsonObject expectedHold, JsonObject actualHold) {
    if (expectedHold.getString("requestId").equals(actualHold.getString("requestId"))) {
      assertEquals(expectedHold.getString("pickupLocationId"), actualHold.getString("pickupLocationId"));
      assertEquals(expectedHold.getString("status"), actualHold.getString("status"));
      assertEquals(expectedHold.getString("expirationDate") == null ? null : new DateTime(expectedHold.getString("expirationDate"), DateTimeZone.UTC),
          actualHold.getString("expirationDate") == null ? null : new DateTime(actualHold.getString("expirationDate"), DateTimeZone.UTC));
      assertEquals(expectedHold.getInteger("requestPosition"),
          actualHold.getInteger("requestPosition"));
      return verifyItem(expectedHold.getJsonObject("item"), actualHold.getJsonObject("item"));
    }

    return false;
  }

  private boolean verifyLoan(JsonObject expectedLoan, JsonObject actualLoan) {
    if (expectedLoan.getString("id").equals(actualLoan.getString("id"))) {
      assertEquals(expectedLoan.getString("loanDate"), actualLoan.getString("loanDate"));
      assertEquals(expectedLoan.getString("dueDate"), actualLoan.getString("dueDate"));
      assertEquals(expectedLoan.getBoolean("overdue"), actualLoan.getBoolean("overdue"));

      return verifyItem(expectedLoan.getJsonObject("item"), actualLoan.getJsonObject("item"));
    }

    return false;
  }

  private boolean verifyItem(JsonObject expectedItem, JsonObject actualItem) {
    if (expectedItem.getString("itemId").equals(actualItem.getString("itemId"))) {
      assertEquals(expectedItem.getString("instanceId"), actualItem.getString("instanceId"));
      assertEquals(expectedItem.getString("title"), actualItem.getString("title"));
      assertEquals(expectedItem.getString("author"), actualItem.getString("author"));

      return true;
    }

    return false;
  }
}
