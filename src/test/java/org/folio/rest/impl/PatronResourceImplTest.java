package org.folio.rest.impl;

import static org.folio.rtac.utils.Utils.readMockFile;

import org.folio.rest.RestVerticle;
import org.folio.rest.tools.PomReader;
import org.folio.rtac.utils.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class PatronResourceImplTest {
  private final Logger logger = LoggerFactory.getLogger(PatronResourceImplTest.class);

  private Vertx vertx;
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
  private final String goodInstanceId = "f39fd3ca-e3fb-4cd9-8cf9-48e7e2c494e5";
  private final String badInstanceId = "114a048c-916a-43fd-a8cb-8eacc296fe01";
  private final String goodHoldId = "dd238b5b-01fc-4205-83b8-ce27a650d827";
  private final String badHoldId = "1745628c-f424-4b50-a116-e18be37cd599";

  private final String chargeItemBook1Id = "e785f572-c5d4-4bbc-91ba-c0d62ebebc20";
  private final String chargeItemBook2Id = "cb958743-ddcd-4bf6-907a-e6962b66bfe9";
  private final String chargeItemBook3Id = "95546593-f846-4df2-8f34-9bf5debbcd10";
  private final String chargeItemCameraId = "5531b437-349c-4453-9361-69082324949f";
  private final String holdingsBook1Id = "ace20b0f-1b35-41ae-8ce2-2c7cc9a98819";
  private final String holdingsBook2Id = "75d0799a-66d8-46cf-a7e3-ed7390425112";
  private final String holdingsBook3Id = "39a2de0a-95a3-4870-9320-57476afc2faf";
  private final String instanceBook1Id = "6e024cd5-c19a-4fe0-a2cd-64ce5814c694";
  private final String instanceBook2Id = "b3f5ef6d-2309-4935-858d-870cd7801632";
  private final String instanceBook3Id = "f3482bed-a7e9-4f07-beb0-ebd693331350";
  private final String book1Barcode = "1234567890";
  private final String book2Barcode = "1234567891";
  private final String book3Barcode = "1234567892";
  private final String cameraBarcode = "1234567893";
  private final String feeFineOverdueId = "cdf3970f-7ed2-4dae-8ae3-a8250a83a9a0";
  private final String feeFineDamageBookId = "881c628b-e1c4-4711-b9d7-090af40f6a8f";
  private final String feeFineDamageEquipmentId = "ca295e87-223f-403c-9eee-a152c47bf67f";

  @Before
  public void setUp(TestContext context) throws Exception {
    vertx = Vertx.vertx();

    moduleName = PomReader.INSTANCE.getModuleName().replaceAll("_", "-");
    moduleVersion = PomReader.INSTANCE.getVersion();
    moduleId = moduleName + "-" + moduleVersion;
    logger.info("Test setup starting for " + moduleId);

    final JsonObject conf = new JsonObject();
    conf.put("http.port", okapiPort);

    final DeploymentOptions opt = new DeploymentOptions().setConfig(conf);
    vertx.deployVerticle(RestVerticle.class.getName(), opt, context.asyncAssertSuccess());
    RestAssured.port = okapiPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    logger.info("Patron Services Test Setup Done using port " + okapiPort);

    final String host = "localhost";

    final Async async = context.async();
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
          req.response()
            .setStatusCode(201)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/holds_create.json"));
        } else {
          if (req.query().equals(String.format("limit=%d&query=%%28requesterId%%3D%%3D%s%%20and%%20requestType%%3D%%3DHold%%20and%%20status%%3D%%3DOpen%%2A%%29", Integer.MAX_VALUE, goodUserId))) {
            req.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(readMockFile(mockDataFolder + "/holds_all.json"));
          } else if (req.query().equals(String.format("limit=%d&query=%%28requesterId%%3D%%3D%s%%20and%%20requestType%%3D%%3DHold%%20and%%20status%%3D%%3DOpen%%2A%%29", 1, goodUserId))) {
            req.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(readMockFile(mockDataFolder + "/holds_totals.json"));
          } else {
            req.response().setStatusCode(500).end("Unexpected call: " + req.path());
          }
        }
      } else if (req.path().equals("/circulation/requests/" + goodHoldId)) {
        if (req.method() == HttpMethod.DELETE) {
          req.response()
            .setStatusCode(204)
            .end();
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
      } else if (req.path().equals("/inventory/items")) {
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
      } else {
        req.response().setStatusCode(500).end("Unexpected call: " + req.path());
      }
    });

    server.listen(serverPort, host, ar -> {
      context.assertTrue(ar.succeeded());
      async.complete();
    });
  }

  @After
  public void tearDown(TestContext context) {
    logger.info("Patron Services Testing Complete");
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public final void testGetPatronAccountById(TestContext context) {
    logger.info("Testing for successful patron services account retrieval by id");
    final Async asyncLocal = context.async();

    final Response r = RestAssured
      .given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", goodUserId)
        .queryParam("includeLoans", "true")
        .queryParam("includeHolds", "true")
        .queryParam("includeCharges", "true")
      .get(accountPath)
        .then()
          .contentType(ContentType.JSON)
          .statusCode(200)
          .extract()
            .response();

    final String body = r.getBody().asString();
    final JsonObject json = new JsonObject(body);
    final JsonObject expectedJson = new JsonObject(readMockFile(mockDataFolder + "/response_testGetPatronAccountById.json"));

    context.assertEquals(3, json.getInteger("totalLoans"));
    context.assertEquals(3, json.getJsonArray("loans").size());

    context.assertEquals(3, json.getInteger("totalHolds"));
    context.assertEquals(3, json.getJsonArray("holds").size());

    JsonObject money = json.getJsonObject("totalCharges");
    context.assertEquals(155.0, money.getDouble("amount"));
    context.assertEquals("USD", money.getString("isoCurrencyCode"));
    context.assertEquals(4, json.getInteger("totalChargesCount"));
    context.assertEquals(4, json.getJsonArray("charges").size());

    for (int i = 0; i < 4; i++) {
      final JsonObject jo = json.getJsonArray("charges").getJsonObject(i);

      boolean found = false;
      for (int j = 0; j < 4; j++) {
        final JsonObject expectedJO = expectedJson.getJsonArray("charges").getJsonObject(j);
        if (verifyCharge(expectedJO, jo, context)) {
          found = true;
          break;
        }
      }

      if (found == false) {
        context.fail("Unexpected charge: " + jo.toString());
      }
    }

    for (int i = 0; i < 3; i++) {
      final JsonObject jo = json.getJsonArray("holds").getJsonObject(i);

      boolean found = false;
      for (int j = 0; j < 3; j++) {
        final JsonObject expectedJO = expectedJson.getJsonArray("holds").getJsonObject(j);
        if (verifyHold(expectedJO, jo, context)) {
          found = true;
          break;
        }
      }

      if (found == false) {
        context.fail("Unexpected id: " + jo.getString("requestId"));
      }
    }

    for (int i = 0; i < 3; i++) {
      final JsonObject jo = json.getJsonArray("loans").getJsonObject(i);

      boolean found = false;
      for (int j = 0; j < 3; j++) {
        final JsonObject expectedJO = expectedJson.getJsonArray("loans").getJsonObject(j);
        if (verifyLoan(expectedJO, jo, context)) {
          found = true;
          break;
        }
      }

      if (found == false) {
        context.fail("Unexpected loan: " + jo.toString());
      }
    }

    asyncLocal.complete();

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testGetPatronAccountByIdNoLists(TestContext context) {
    logger.info("Testing for successful patron services account retrieval by id without item lists");
    final Async asyncLocal = context.async();

    final Response r = RestAssured
      .given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", goodUserId)
      .get(accountPath)
        .then()
          .contentType(ContentType.JSON)
          .statusCode(200)
          .extract()
            .response();

    final String body = r.getBody().asString();
    final JsonObject json = new JsonObject(body);

    context.assertEquals(3, json.getInteger("totalLoans"));
    context.assertEquals(0, json.getJsonArray("loans").size());

    context.assertEquals(3, json.getInteger("totalHolds"));
    context.assertEquals(0, json.getJsonArray("holds").size());

    JsonObject money = json.getJsonObject("totalCharges");
    context.assertEquals(155.0, money.getDouble("amount"));
    context.assertEquals("USD", money.getString("isoCurrencyCode"));
    context.assertEquals(4, json.getInteger("totalChargesCount"));
    context.assertEquals(0, json.getJsonArray("charges").size());

    asyncLocal.complete();

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testGetPatronAccountById400UserNotActive(TestContext context) {
    logger.info("Testing for 400 due to patron account not active");
    final Async asyncLocal = context.async();

    RestAssured
      .given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", inactiveUserId)
      .get(accountPath)
        .then()
          .contentType(ContentType.TEXT)
          .statusCode(400);

    asyncLocal.complete();

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testGetPatronAccountById404(TestContext context) {
    logger.info("Testing for 404 due to unknown user id");
    final Async asyncLocal = context.async();

    RestAssured
      .given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", badUserId)
      .get(accountPath)
        .then()
          .contentType(ContentType.TEXT)
          .statusCode(404);

    asyncLocal.complete();

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testPostPatronAccountByIdItemByItemIdRenew(TestContext context) {
    logger.info("Testing renew for 501");
    final Async asyncLocal = context.async();

    RestAssured
      .given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", badUserId)
        .pathParam("itemId", "c9b8958e-dea6-4547-843f-02001d5265ff")
      .post(accountPath + itemPath + renewPath)
        .then()
          .statusCode(501);

    asyncLocal.complete();

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testPostPatronAccountByIdItemByItemIdHold(TestContext context) {
    logger.info("Testing creating a hold on an item for the specified user");
    final Async asyncLocal = context.async();

    final Response r = RestAssured
      .given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .body(readMockFile(mockDataFolder + "/request_testPostPatronAccountByIdItemByItemIdHold.json"))
        .pathParam("accountId", goodUserId)
        .pathParam("itemId", goodItemId)
      .post(accountPath + itemPath + holdPath)
        .then()
          .contentType(ContentType.JSON)
          .statusCode(201)
          .extract()
            .response();

    final String body = r.getBody().asString();
    final JsonObject json = new JsonObject(body);
    final JsonObject expectedJson = new JsonObject(readMockFile(mockDataFolder + "/response_testPostPatronAccountByIdItemByItemIdHold.json"));

    verifyHold(expectedJson, json, context);

    asyncLocal.complete();

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testPutPatronAccountByIdItemByItemIdHoldByHoldId(TestContext context) {
    logger.info("Testing edit hold for 501");
    final Async asyncLocal = context.async();

    RestAssured
      .given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", badUserId)
        .pathParam("itemId", "c9b8958e-dea6-4547-843f-02001d5265ff")
        .pathParam("holdId", "1745628c-f424-4b50-a116-e18be37cd599")
      .put(accountPath + itemPath + holdPath + holdIdPath)
        .then()
          .statusCode(501);

    asyncLocal.complete();

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testDeletePatronAccountByIdItemByItemIdHoldByHoldId(TestContext context) {
    logger.info("Testing delete hold by id");
    final Async asyncLocal = context.async();

    RestAssured
      .given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", goodUserId)
        .pathParam("itemId", goodItemId)
        .pathParam("holdId", goodHoldId)
      .delete(accountPath + itemPath + holdPath + holdIdPath)
        .then()
          .statusCode(204);

    asyncLocal.complete();

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testDeletePatronAccountByIdItemByItemIdHoldByHoldId404(TestContext context) {
    logger.info("Testing delete hold by with an unknown id");
    final Async asyncLocal = context.async();

    RestAssured
      .given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", goodUserId)
        .pathParam("itemId", goodItemId)
        .pathParam("holdId", badHoldId)
      .delete(accountPath + itemPath + holdPath + holdIdPath)
        .then()
          .statusCode(404);

    asyncLocal.complete();

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testPostPatronAccountByIdInstanceByInstanceIdHold(TestContext context) {
    logger.info("Testing creating a hold on an instance for the specified user (501)");
    final Async asyncLocal = context.async();

    RestAssured
      .given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", goodUserId)
        .pathParam("instanceId", goodInstanceId)
      .post(accountPath + instancePath + holdPath)
        .then()
          .statusCode(501);

    asyncLocal.complete();

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testPutPatronAccountByIdInstanceByInstanceIdHoldByHoldId(TestContext context) {
    logger.info("Testing edit hold (instance) for 501");
    final Async asyncLocal = context.async();

    RestAssured
      .given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", badUserId)
        .pathParam("instanceId", badInstanceId)
        .pathParam("holdId", "1745628c-f424-4b50-a116-e18be37cd599")
      .put(accountPath + instancePath + holdPath + holdIdPath)
        .then()
          .statusCode(501);

    asyncLocal.complete();

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testDeletePatronAccountByIdInstanceByInstanceIdHoldByHoldId(TestContext context) {
    logger.info("Testing delete hold (instance) by id");
    final Async asyncLocal = context.async();

    RestAssured
      .given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", goodUserId)
        .pathParam("instanceId", goodInstanceId)
        .pathParam("holdId", goodHoldId)
      .delete(accountPath + instancePath + holdPath + holdIdPath)
        .then()
          .statusCode(501);

    asyncLocal.complete();

    // Test done
    logger.info("Test done");
  }

  private boolean verifyCharge(JsonObject expectedCharge, JsonObject actualCharge, TestContext context) {
    // Bad check, but each date is unique in the mock data.
    if (expectedCharge.getString("accrualDate").equals(actualCharge.getString("accrualDate"))) {
      context.assertEquals(expectedCharge.getString("state"), actualCharge.getString("state"));
      context.assertEquals(expectedCharge.getString("reason"), actualCharge.getString("reason"));
      context.assertEquals(expectedCharge.getString("feeFineId"), actualCharge.getString("feeFineId"));

      verifyAmount(expectedCharge.getJsonObject("chargeAmount"), actualCharge.getJsonObject("chargeAmount"), context);

      return verifyItem(expectedCharge.getJsonObject("item"), actualCharge.getJsonObject("item"), context);
    }

    return false;
  }

  private void verifyAmount(JsonObject expectedAmount, JsonObject actualAmount, TestContext context) {
    context.assertEquals(expectedAmount.getDouble("amount"), actualAmount.getDouble("amount"));
    context.assertEquals(expectedAmount.getString("isoCurrencyCode"), actualAmount.getString("isoCurrencyCode"));
  }

  private boolean verifyHold(JsonObject expectedHold, JsonObject actualHold, TestContext context) {
    if (expectedHold.getString("requestId").equals(actualHold.getString("requestId"))) {
      context.assertEquals(expectedHold.getString("fulfillmentPreference"), actualHold.getString("fulfillmentPreference"));
      context.assertEquals(expectedHold.getString("status"), actualHold.getString("status"));
      context.assertEquals(expectedHold.getString("expirationDate"), actualHold.getString("expirationDate"));

      return verifyItem(expectedHold.getJsonObject("item"), actualHold.getJsonObject("item"), context);
    }

    return false;
  }

  private boolean verifyLoan(JsonObject expectedLoan, JsonObject actualLoan, TestContext context) {
    if (expectedLoan.getString("id").equals(actualLoan.getString("id"))) {
      context.assertEquals(expectedLoan.getString("loanDate"), actualLoan.getString("loanDate"));
      context.assertEquals(expectedLoan.getString("dueDate"), actualLoan.getString("dueDate"));
      context.assertEquals(expectedLoan.getBoolean("overdue"), actualLoan.getBoolean("overdue"));

      return verifyItem(expectedLoan.getJsonObject("item"), actualLoan.getJsonObject("item"), context);
    }

    return false;
  }

  private boolean verifyItem(JsonObject expectedItem, JsonObject actualItem, TestContext context) {
    if (expectedItem.getString("itemId").equals(actualItem.getString("itemId"))) {
      context.assertEquals(expectedItem.getString("instanceId"), actualItem.getString("instanceId"));
      context.assertEquals(expectedItem.getString("title"), actualItem.getString("title"));
      context.assertEquals(expectedItem.getString("author"), actualItem.getString("author"));

      return true;
    }

    return false;
  }
}
