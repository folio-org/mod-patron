package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.ContentType.TEXT;
import static org.folio.patron.utils.Utils.readMockFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.UrlDecoder;
import org.folio.patron.utils.Utils;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.tools.utils.ModuleName;
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
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
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
  private final String cancelPath = "/cancel";
  private final String okapiBadDataHeader = "x-okapi-bad-data";

  private final String goodUserId = "1ec54964-70f0-44cc-bd19-2a892ea0d336";
  private final String inactiveUserId = "4a87f60c-ebb1-4726-a9b2-548cdd17bbd4";
  private final String badUserId = "3ed07e77-a5c9-47c8-bb0b-381099e10a42";
  private final String goodItemId = "32e5757d-6566-466e-b69d-994eb33d2b62";
  private final String badItemId = "3dda4eb9-a156-474c-829f-bd5a386f382c";
  private final String goodInstanceId = "f39fd3ca-e3fb-4cd9-8cf9-48e7e2c494e5";
  private final String badInstanceId = "68cb9692-aa5f-459d-8791-f79486c11225";
  private final String goodCancelHoldId = "dd238b5b-01fc-4205-83b8-888888888888";
  private final String badCancelHoldId = "dd238b5b-01fc-4205-83b8-999999999999";

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
  private final String feeFineNoItemId = "771b629b-e2c4-4711-b9d7-091af40f6b8b";
  private final String feeFineOverdueId = "cdf3970f-7ed2-4dae-8ae3-a8250a83a9a0";
  private final String feeFineDamageBookId = "881c628b-e1c4-4711-b9d7-090af40f6a8f";
  private final String feeFineDamageEquipmentId = "ca295e87-223f-403c-9eee-a152c47bf67f";
  private final String requestPolicyIdAll = "e4c3b92c-ddb6-4006-a0fd-20fab52b95b9";
  private final String requestPolicyIdHold = "e4c3b92c-ddb6-4006-a0fd-20fab52b95c9";
  private final String requestPolicyIdPage = "e4c3b92c-ddb6-4006-a0fd-20fab52b95d9";
  private final String materialTypeId1 = "1a54b431-2e4f-452d-9cae-9cee66c9a892";
  private final String materialTypeId2 = "1a54b431-2e4f-452d-9cae-9cee66c99992";
  private final String materialTypeId3 = "1a54b431-2e4f-452d-9cae-9cee66c99999";
  private final String loanTypeId1 = "2b94c631-fca9-4892-a730-03ee529ffe27";
  private final String patronGroupId1 = "3684a786-6671-4268-8ed0-9db82ebca60b";
  private final String effectiveLocation1 = "fcd64ce1-6995-48f0-840e-89ffa2288371";
  private final String intransitItemId = "32e5757d-6566-466e-b69d-994eb33d2c98";
  private static final String availableItemId = "32e5757d-6566-466e-b69d-994eb33d2b62";
  private static final String checkedoutItemId = "32e5757d-6566-466e-b69d-994eb33d2b73";
  private static final String sortByParam = "item.title/sort.ascending";

  static {
    System.setProperty("vertx.logger-delegate-factory-class-name",
        "io.vertx.core.logging.Log4j2LogDelegateFactory");
  }

  @BeforeEach
  public void setUp(Vertx vertx, VertxTestContext context) throws Exception {
    vertx.exceptionHandler(context::failNow);

    moduleName = ModuleName.getModuleName().replaceAll("_", "-");
    moduleVersion = ModuleName.getModuleVersion();
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
        if (isInactiveUser(req)) {
          req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/loans_all_inactive.json"));
        }
        else if (loansParametersMatch(req, Integer.MAX_VALUE)) {
          req.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/loans_all_active.json"));
        } else if (loansParametersMatch(req, 1)) {
          req.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/loans_totals.json"));
        } else if (loansParametersWithSortByMatch(req, Integer.MAX_VALUE)) {
          req.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/loans_all_active_and_sorted.json"));
        } else {
          req.response().setStatusCode(500).end("Unexpected call: " + req.path());
        }
      } else if (req.path().equals("/circulation/requests")) {
        if (req.method() == HttpMethod.POST) {
          final String badDataValue = req.getHeader("x-okapi-bad-data");
          if (req.getHeader("x-okapi-bad-item-id") != null) {
            req.response()
                .setStatusCode(422)
                .putHeader("content-type", "application/json")
                .end(readMockFile(mockDataFolder + "/item_hold_bad_item_id.json"));
          } else if (badDataValue != null) {
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
            req.bodyHandler( body -> {
              String content = new String(body.getBytes());
              JsonObject jsonContent = new JsonObject(content);

              if (jsonContent != null) {
                String itemId = jsonContent.getString("itemId");
                RequestType requestType = RequestType.from(jsonContent.getString("requestType"));

                var responsePayload = StringUtils.EMPTY;

                if (itemId.equals(checkedoutItemId) && requestType == RequestType.HOLD) {
                  responsePayload = readMockFile(mockDataFolder + "/holds_create.json");
                } else if (itemId.equals(availableItemId) && requestType == RequestType.PAGE) {
                  responsePayload = readMockFile(mockDataFolder + "/page_create.json");
                }

                req.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json")
                    .end(responsePayload);
              }
            });
          }
        } else {
          if (isInactiveUser(req)) {
            req.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/holds_all_inactive.json"));
          } else if (requestsParametersMatch(req, Integer.MAX_VALUE)) {
            req.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(readMockFile(mockDataFolder + "/holds_all_active.json"));
          } else if (requestsParametersMatch(req, 1)) {
            req.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(readMockFile(mockDataFolder + "/holds_totals.json"));
          } else if (requestsParametersWithSortByMatchMatch(req, Integer.MAX_VALUE)) {
            req.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(readMockFile(mockDataFolder + "/holds_all_active_and_sorted.json"));
          } else {
            req.response().setStatusCode(500).end("Unexpected call: " + req.path());
          }
        }
      } else if (req.path().equals("/circulation/requests/instances")) {
        if (req.method() == HttpMethod.POST) {
          final String badDataValue = req.getHeader("x-okapi-bad-data");
          if (req.getHeader("x-okapi-bad-instance-id") != null) {
            req.response()
                .setStatusCode(422)
                .putHeader("content-type", "application/json")
                .end(readMockFile(mockDataFolder + "/instance_hold_bad_instance_id.json"));
          } else if (badDataValue != null) {
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
      } else if (req.path().equals("/circulation/requests/" + goodCancelHoldId)) {
        final String badDataValue = req.getHeader(okapiBadDataHeader);
        if (req.method() == HttpMethod.PUT) {
          if (badDataValue != null && badDataValue.equals("422")) {
            req.response()
              .setStatusCode(422)
              .putHeader("content-type", "application/json")
              .end(readMockFile(mockDataFolder + "/hold_cancel_error.json"));
          } else {
            req.response()
              .setStatusCode(204)
              .end();
          }
        } else if (req.method() == HttpMethod.GET) {
          if (badDataValue != null && badDataValue.equals("hold_cancel_malformed.json")) {
            req.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(readMockFile(mockDataFolder + "/hold_cancel_error_malformed.json"));
          } if (badDataValue != null && badDataValue.equals("good-hold-cancel-wo-cancel-date")) {
            String responseBody = readMockFile(mockDataFolder + "/hold_cancel_without_cancel_date.json");
            req.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(responseBody);
          } else {
            String responseBody = readMockFile(mockDataFolder + "/hold_cancel.json");
            req.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(responseBody);
          }
        }
      } else if (req.path().equals("/circulation/requests/"  + badCancelHoldId)) {
        final String badDataValue = req.getHeader("x-okapi-bad-data");
        if (badDataValue.equals("404")) {
          req.response()
            .setStatusCode(404)
            .putHeader("content-type", "text/plain")
            .end("hold not found");
        } else if (badDataValue.equals("403")) {
          req.response()
            .setStatusCode(403)
            .putHeader("content-type", "text/plain")
            .end("access denied");
        } else if (badDataValue.equals("401")) {
          req.response()
            .setStatusCode(401)
            .putHeader("content-type", "text/plain")
            .end("unable to cancel hold -- unauthorized");
        } else if (badDataValue.equals("400")) {
          req.response()
            .setStatusCode(400)
            .putHeader("content-type", "text/plain")
            .end("unable to process request -- constraint violation");
        } else {
          req.response()
            .setStatusCode(500)
            .end("internal server error, contact administrator");
        }
      } else if (req.path().equals("/accounts")) {
        if (isInactiveUser(req)) {
          req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/accounts_all_inactive.json"));
        } else if (accountParametersMatch(req)) {
          req.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/accounts_all_active.json"));
        } else if (accountParametersWithSortByMatch(req)) {
          req.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/accounts_all_active_and_sorted.json"));
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
      } else if (req.path().equals("/inventory/instances")) {
        if (req.query() == null || ! req.query().matches("query=holdingsRecords\\.id%3D%3D%22.*%22")) {
          req.response().setStatusCode(500).end("Unexpected /inventory/instances query: " + req.query());
          return;
        }
        String file;
        String id = req.query().substring(33, req.query().length() - 3);
        switch (id) {
        case holdingsBook1Id:  file = "/instance_book1.json";  break;
        case holdingsBook2Id:  file = "/instance_book2.json";  break;
        case holdingsBook3Id:  file = "/instance_book3.json";  break;
        case holdingsCameraId: file = "/instance_camera.json"; break;
        default:
          req.response().setStatusCode(500).end(
              "Unexpected holdings id " + id + " in /inventory/instances query: " + req.query());
          return;
        }
        JsonObject instance = new JsonObject(readMockFile(mockDataFolder + file));
        JsonObject instances = new JsonObject().put("instances", new JsonArray().add(instance));
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(instances.encodePrettily());
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
      } else if (req.path().equals("/inventory/items/" + goodItemId)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/item_book4.json"));
      } else if (req.path().equals("/inventory/items/" + checkedoutItemId)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/item_checkedout.json"));
      }  else if (req.path().equals("/inventory/items/" + intransitItemId)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/item_intransit.json"));
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
      } else if (req.path().equals("/feefines/" + feeFineNoItemId)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/feefine_no_item.json"));
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
      } else if (req.path().equals("/circulation/rules/request-policy")) {
          // These checks require that the query string parameters be produced in a specific order
          if (rulesParametersMatch(req, materialTypeId1)) {
            req.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(readMockFile(mockDataFolder + "/requestPolicyId_all.json"));
          } else if (rulesParametersMatch(req, materialTypeId2)) {
            req.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(readMockFile(mockDataFolder + "/requestPolicyId_hold.json"));
          } else if (rulesParametersMatch(req, materialTypeId3)) {
            req.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(readMockFile(mockDataFolder + "/requestPolicyId_page.json"));
          }
      } else if (req.path().contains("/request-policy-storage/request-policies/" + requestPolicyIdAll)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/requestPolicy_all.json"));
      } else if (req.path().contains("/request-policy-storage/request-policies/" + requestPolicyIdHold)) {
          req.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(readMockFile(mockDataFolder + "/requestPolicy_hold.json"));
      } else if (req.path().contains("/request-policy-storage/request-policies/" + requestPolicyIdPage)) {
        req.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(readMockFile(mockDataFolder + "/requestPolicy_page.json"));
      } else {
        req.response().setStatusCode(500).end("Unexpected call: " + req.path());
      }
    });
    server.listen(serverPort, host, context.succeeding(id -> mockOkapiStarted.flag()));
  }

  private boolean accountParametersMatch(HttpServerRequest request) {
    final var queryString = UrlDecoder.decode(request.query());

    return queryString.contains("limit=" + Integer.MAX_VALUE)
      && queryString.contains(String.format("query=(userId==%s and status.name==Open)", goodUserId));
  }

  private boolean accountParametersWithSortByMatch(HttpServerRequest request) {
    final var queryString = UrlDecoder.decode(request.query());

    return queryString.contains("limit=" + Integer.MAX_VALUE)
      && queryString
      .contains(String.format("query=(userId==%s and status.name==Open) sortBy %s", goodUserId, sortByParam));
  }

  private boolean requestsParametersMatch(HttpServerRequest request, int limit) {
    final var queryString = UrlDecoder.decode(request.query());

    return queryString.contains("limit=" + limit)
      && queryString.contains(String.format("query=(requesterId==%s and status==Open*)", goodUserId));
  }

  private boolean requestsParametersWithSortByMatchMatch(HttpServerRequest request, int limit) {
    final var queryString = UrlDecoder.decode(request.query());

    return queryString.contains("limit=" + limit)
      && queryString
      .contains(String.format("query=(requesterId==%s and status==Open*) sortBy %s", goodUserId, sortByParam));
  }

  private Boolean isInactiveUser(HttpServerRequest request) {
    final var queryString = UrlDecoder.decode(request.query());
    return queryString.contains(inactiveUserId);
  }

  private boolean loansParametersMatch(HttpServerRequest request, int limit) {
    final var queryString = UrlDecoder.decode(request.query());

    return queryString.contains("limit=" + limit)
      && queryString.contains(String.format("query=(userId==%s and status.name==Open)", goodUserId));
  }

  private boolean loansParametersWithSortByMatch(HttpServerRequest request, int limit) {
    final var queryString = UrlDecoder.decode(request.query());

    return queryString.contains("limit=" + limit) && queryString
      .contains(String.format("query=(userId==%s and status.name==Open) sortBy %s", goodUserId, sortByParam));
  }

  private boolean rulesParametersMatch(HttpServerRequest request, String materialTypeId) {
    final var queryString = request.query();

    return queryString.contains("item_type_id=" + materialTypeId)
      && queryString.contains("loan_type_id=" + loanTypeId1)
      && queryString.contains("patron_type_id=" + patronGroupId1)
      && queryString.contains("location_id=" + effectiveLocation1);
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

    verifyAccount(json, expectedJson);

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testGetPatronAccountByIdSortedByItemId() {
    logger.info("Testing for successful patron services account retrieval by id and sorted by item title");

    final Response r = given()
        .log().all()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .pathParam("accountId", goodUserId)
        .queryParam("includeLoans", "true")
        .queryParam("includeHolds", "true")
        .queryParam("includeCharges", "true")
        .queryParam("sortBy", sortByParam)
      .when()
        .get(accountPath)
      .then()
        .log().all()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .extract().response();

    final String body = r.getBody().asString();
    final JsonObject json = new JsonObject(body);
    final JsonObject expectedJson = new JsonObject(
      readMockFile(mockDataFolder + "/response_testGetPatronAccountById_sortedByItemTitle.json"));

    verifyAccount(json, expectedJson);

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
    assertEquals(255.0, money.getDouble("amount"));
    assertEquals("USD", money.getString("isoCurrencyCode"));
    assertEquals(5, json.getInteger("totalChargesCount"));
    assertEquals(0, json.getJsonArray("charges").size());

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testGetPatronAccountByIdUserNotActive() {
    logger.info("Testing successful patron services account retrieval by id for inactive patron");

    final Response r = given()
      .header(tenantHeader)
      .header(urlHeader)
      .header(contentTypeHeader)
      .pathParam("accountId", inactiveUserId)
    .when()
      .get(accountPath)
    .then()
      .log().all()
      .contentType(ContentType.JSON)
      .statusCode(200)
      .extract().response();

    final String body = r.getBody().asString();
    final JsonObject json = new JsonObject(body);

    assertEquals(1, json.getInteger("totalLoans"));
    assertEquals(0, json.getJsonArray("loans").size());

    assertEquals(0, json.getInteger("totalHolds"));
    assertEquals(0, json.getJsonArray("holds").size());

    final JsonObject money = json.getJsonObject("totalCharges");
    assertEquals(100.0, money.getDouble("amount"));
    assertEquals("USD", money.getString("isoCurrencyCode"));
    assertEquals(1, json.getInteger("totalChargesCount"));
    assertEquals(0, json.getJsonArray("charges").size());

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

  @ParameterizedTest
  @MethodSource("itemRequestsParams")
  public final void testPostPatronAccountByIdItemByItemIdRequests(String itemId, String responseFile) {
    logger.info("Testing creating a hold on an item for the specified user");

    final Response r = given()
        .log().all()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .body(readMockFile(mockDataFolder + "/request_testPostPatronAccountByIdItemByItemIdHold.json"))
        .pathParam("accountId", goodUserId)
        .pathParam("itemId", itemId)
      .when()
        .post(accountPath + itemPath + holdPath)
      .then()
        .log().all()
        .contentType(ContentType.JSON)
        .statusCode(201)
        .extract().response();

    final String body = r.getBody().asString();
    final JsonObject json = new JsonObject(body);
    final JsonObject expectedJson = new JsonObject(readMockFile(mockDataFolder + responseFile));

    verifyRequests(expectedJson, json);

    // Test done
    logger.info("Test done");
  }

  /*
  This test checks the negative case of not being able to place a request due to request policy and whitelist restrictions
   */
  @Test
  public final void testCannotPostPatronAccountByIdItemByItemIdRequest() {
    logger.info("Testing creating a page request on an item for the specified user");

    final Response r = given()
      .log().all()
      .header(tenantHeader)
      .header(urlHeader)
      .header(contentTypeHeader)
      .body(readMockFile(mockDataFolder + "/request_testPostPatronAccountByIdItemByItemIdHold.json"))
      .pathParam("accountId", goodUserId)
      .pathParam("itemId", intransitItemId)
      .when()
      .post(accountPath + itemPath + holdPath)
      .then()
      .log().all()
      .contentType(ContentType.JSON)
      .statusCode(422)
      .extract().response();

    final String body = r.getBody().asString();
    final Errors errors = Json.decodeValue(body, Errors.class);

    final String expectedMessage = "Cannot find a valid request type for this item";
    assertNotNull(errors);
    assertNotNull(errors.getErrors());
    assertEquals(1, errors.getErrors().size());
    assertEquals(expectedMessage, errors.getErrors().get(0).getMessage());

    assertNotNull(errors.getErrors().get(0).getParameters());
    assertEquals(1, errors.getErrors().get(0).getParameters().size());
    assertEquals("itemId", errors.getErrors().get(0).getParameters().get(0).getKey());
    assertEquals(intransitItemId, errors.getErrors().get(0).getParameters().get(0).getValue());

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testSuccessfulCancelPatronRequestByHoldId() {
    logger.info("Testing cancellation hold by id");

    String aBody = readMockFile(mockDataFolder + "/hold_cancel_request.json");

    final var holdCancelResponse = given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .body(aBody)
        .pathParam("accountId", goodUserId)
        .pathParam("holdId", goodCancelHoldId)
        .log().all()
      .when()
        .contentType(ContentType.JSON)
        .post(accountPath + holdPath + holdIdPath + cancelPath)
      .then()
        .log().all()
        .and().assertThat().contentType(ContentType.JSON)
        .and().assertThat().statusCode(200)
      .extract()
        .asString();

    final var expectedHold = new JsonObject(readMockFile(mockDataFolder + "/response_testPostPatronAccountByIdItemByItemIdHoldCancel.json"));

    verifyHold(expectedHold, new JsonObject(holdCancelResponse));

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testSuccessfulCancelPatronRequestByHoldIdWithoutCancelDate() {
    logger.info("Testing cancellation hold by id");

    String aBody = readMockFile(mockDataFolder + "/hold_cancel_request.json");
    JsonObject jsonBody = new JsonObject(aBody);
    jsonBody.remove("cancelledDate");
    aBody = jsonBody.encodePrettily();

    final var holdCancelResponse = given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .header(new Header(okapiBadDataHeader, "good-hold-cancel-wo-cancel-date"))
        .body(aBody)
        .pathParam("accountId", goodUserId)
        .pathParam("holdId", goodCancelHoldId)
        .log().all()
      .when()
        .contentType(ContentType.JSON)
        .post(accountPath + holdPath + holdIdPath + cancelPath)
      .then()
        .log().all()
        .and().assertThat().contentType(ContentType.JSON)
        .and().assertThat().statusCode(200)
      .extract()
        .asString();

    final var expectedHold = new JsonObject(readMockFile(mockDataFolder + "/response_testPostPatronAccountByIdItemByItemIdHoldCancel.json"));

    verifyHold(expectedHold, new JsonObject(holdCancelResponse));

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testCancelPatronRequestByHoldIdFailure422() {
    logger.info("Testing cancellation hold by id");

    String aBody = readMockFile(mockDataFolder + "/hold_cancel_request.json");

    final Errors holdErrorResponse = given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(new Header(okapiBadDataHeader, "422"))
        .header(contentTypeHeader)
        .body(aBody)
        .pathParam("accountId", goodUserId)
        .pathParam("holdId", goodCancelHoldId)
        .log().all()
      .when()
        .contentType(ContentType.JSON)
        .post(accountPath + holdPath + holdIdPath + cancelPath)
        .then()
        .log().all()
        .and().assertThat().contentType(ContentType.JSON)
        .and().assertThat().statusCode(422)
      .extract()
        .as(Errors.class);

    assertEquals(1, holdErrorResponse.getErrors().size());
    assertEquals("Cannot edit a closed request",
      holdErrorResponse.getErrors().get(0).getMessage());
    assertEquals(1, holdErrorResponse.getErrors().get(0).getParameters().size());
    assertEquals("id",
      holdErrorResponse.getErrors().get(0).getParameters().get(0).getKey());
    assertEquals("69f059dd-e8ad-43a9-b2d7-d35a0ad3ab1b",
      holdErrorResponse.getErrors().get(0).getParameters().get(0).getValue());

    // Test done
    logger.info("Test done");
  }

  @Test
  public final void testPostPatronAccountByIdInstanceByInstanceIdHold() {
    logger.info("Testing creating a hold on an instance for the specified user");

    final var hold = given()
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
        .asString();

    final var expectedHold = new JsonObject(readMockFile(mockDataFolder + "/response_testPostPatronAccountByIdInstanceByInstanceIdHold.json"));

    verifyHold(expectedHold, new JsonObject(hold));

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

  @Test
  public final void postToCreateInstanceHoldRequestShouldForwardOn422Error() {
    logger.info("Testing creating a hold on an instance for the specified user with bad instance id error");

    final var holdErrorResponse = given()
        .headers(new Headers(tenantHeader, urlHeader, contentTypeHeader,
            new Header("x-okapi-bad-instance-id", badInstanceId),
            new Header(okapiBadDataHeader, "422")))
        .and().pathParams("accountId", goodUserId, "instanceId", badInstanceId)
        .and().body(readMockFile(mockDataFolder
        + "/request_testPostPatronAccountByIdInstanceByInstanceIdHold.json"))
      .when()
        .contentType(ContentType.JSON)
        .post(accountPath + instancePath + holdPath)
        .then()
        .log().all()
        .and().assertThat().contentType(ContentType.JSON)
        .and().assertThat().statusCode(422)
      .extract()
        .as(Errors.class);

    assertEquals(1, holdErrorResponse.getErrors().size());
    assertEquals("No instance with ID 68cb9692-aa5f-459d-8791-f79486c11225 exists",
      holdErrorResponse.getErrors().get(0).getMessage());
    assertEquals(1, holdErrorResponse.getErrors().get(0).getParameters().size());
    assertEquals("instanceId",
      holdErrorResponse.getErrors().get(0).getParameters().get(0).getKey());
    assertEquals("68cb9692-aa5f-459d-8791-f79486c11225",
      holdErrorResponse.getErrors().get(0).getParameters().get(0).getValue());
  }

  @Test
  public final void postToCreateItemHoldRequestShouldForwardOn422Error() {
    logger.info("Testing creating a hold on an item for the specified user with bad item id error");

    final var holdErrorResponse = given()
        .headers(new Headers(tenantHeader, urlHeader, contentTypeHeader,
            new Header("x-okapi-bad-item-id", badItemId),
            new Header(okapiBadDataHeader, "422")))
        .and().pathParams("accountId", goodUserId, "itemId", goodItemId)
        .and().body(readMockFile(mockDataFolder
        + "/request_testPostPatronAccountByIdItemByItemIdHold.json"))
        .when()
        .post(accountPath + itemPath + holdPath)
        .then()
        .log().all()
        .and().assertThat().contentType(ContentType.JSON)
        .and().assertThat().statusCode(422)
        .extract()
        .as(Errors.class);

    assertEquals(1, holdErrorResponse.getErrors().size());
    assertEquals("No item with ID 3dda4eb9-a156-474c-829f-bd5a386f382c",
      holdErrorResponse.getErrors().get(0).getMessage());
    assertEquals(1, holdErrorResponse.getErrors().get(0).getParameters().size());
    assertEquals("itemId",
      holdErrorResponse.getErrors().get(0).getParameters().get(0).getKey());
    assertEquals("3dda4eb9-a156-474c-829f-bd5a386f382c",
      holdErrorResponse.getErrors().get(0).getParameters().get(0).getValue());
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
  @MethodSource("itemHoldsCancelFailureCodes")
  public final void testDeletePatronAccountByIdItemByItemIdHoldByHoldIdWithErrors(
      String codeString, int expectedCode, String errorText) {
    logger.info("Testing cancelling a hold on an item for the specified user with a {} error",
        codeString);

    String response = given()
        .header(tenantHeader)
        .header(urlHeader)
        .header(contentTypeHeader)
        .header(new Header(okapiBadDataHeader, codeString))
        .pathParam("accountId", goodUserId)
        .pathParam("holdId", badCancelHoldId)
        .body(readMockFile(mockDataFolder + "/generic_hold_cancel_request.json"))
      .when()
        .post(accountPath + holdPath + holdIdPath + cancelPath)
      .then()
        .log().all()
        .statusCode(expectedCode)
        .extract().body().asString();

    assertEquals(errorText, response);
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

  static Stream<Arguments> itemHoldsCancelFailureCodes() {
    return Stream.of(
        // Even though we receive a 400, we need to return a 500 since there is nothing the client
        // can do to correct the 400. We'd have to correct it in the code.
        Arguments.of("400", 400, "unable to process request -- constraint violation"),
        Arguments.of("401", 401, "unable to cancel hold -- unauthorized"),
        Arguments.of("403", 403, "access denied"),
        Arguments.of("404", 404, "hold not found"),
        Arguments.of("500", 500, "internal server error, contact administrator")
      );
  }

  static Stream<Arguments> itemRequestsParams() {
    return Stream.of(
      Arguments.of(availableItemId, "/response_testPostPatronAccountByIdItemByItemIdPage.json" ),
      Arguments.of(checkedoutItemId, "/response_testPostPatronAccountByIdItemByItemIdHold.json")
    );
  }

  private boolean verifyCharge(JsonObject expectedCharge, JsonObject actualCharge) {
    // Bad check, but each date is unique in the mock data.
    if (expectedCharge.getString("accrualDate").equals(actualCharge.getString("accrualDate"))) {
      assertEquals(expectedCharge.getString("state"), actualCharge.getString("state"));
      assertEquals(expectedCharge.getString("reason"), actualCharge.getString("reason"));
      verifyAmount(expectedCharge.getJsonObject("chargeAmount"), actualCharge.getJsonObject("chargeAmount"));
      if (expectedCharge.getJsonObject("item") != null) {
        return verifyItem(expectedCharge.getJsonObject("item"), actualCharge.getJsonObject("item"));
      } else {
        return true;
      }
    }

    return false;
  }

  private void verifyAccount(JsonObject actualAccountJson, JsonObject expectedAccountJson) {
    assertEquals(3, actualAccountJson.getInteger("totalLoans"));
    assertEquals(3, actualAccountJson.getJsonArray("loans").size());

    assertEquals(3, actualAccountJson.getInteger("totalHolds"));
    assertEquals(3, actualAccountJson.getJsonArray("holds").size());

    JsonObject money = actualAccountJson.getJsonObject("totalCharges");
    assertEquals(255.0, money.getDouble("amount"));
    assertEquals("USD", money.getString("isoCurrencyCode"));
    assertEquals(5, actualAccountJson.getInteger("totalChargesCount"));
    assertEquals(5, actualAccountJson.getJsonArray("charges").size());

    for (int i = 0; i < 5; i++) {
      final JsonObject jo = actualAccountJson.getJsonArray("charges").getJsonObject(i);

      boolean found = false;
      for (int j = 0; j < 5; j++) {
        final JsonObject expectedJO = expectedAccountJson.getJsonArray("charges").getJsonObject(j);
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
      final JsonObject jo = actualAccountJson.getJsonArray("holds").getJsonObject(i);

      boolean found = false;
      for (int j = 0; j < 3; j++) {
        final JsonObject expectedJO = expectedAccountJson.getJsonArray("holds").getJsonObject(j);
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
      final JsonObject jo = actualAccountJson.getJsonArray("loans").getJsonObject(i);

      boolean found = false;
      for (int j = 0; j < 3; j++) {
        final JsonObject expectedJO = expectedAccountJson.getJsonArray("loans").getJsonObject(j);
        if (verifyLoan(expectedJO, jo)) {
          found = true;
          break;
        }
      }

      if (found == false) {
        fail("Unexpected loan: " + jo.toString());
      }
    }
  }

  private void verifyAmount(JsonObject expectedAmount, JsonObject actualAmount) {
    assertEquals(expectedAmount.getDouble("amount"), actualAmount.getDouble("amount"));
    assertEquals(expectedAmount.getString("isoCurrencyCode"), actualAmount.getString("isoCurrencyCode"));
  }

  private void verifyRequests(JsonObject expectedHold, JsonObject actualHold) {
    if (!verifyHold(expectedHold, actualHold)) {
      fail("verification of request objects failed");
    }
  }

  private boolean verifyHold(JsonObject expectedHold, JsonObject actualHold) {
    if (expectedHold.getString("requestId").equals(actualHold.getString("requestId"))) {
      assertEquals(expectedHold.getString("pickupLocationId"), actualHold.getString("pickupLocationId"));
      assertEquals(expectedHold.getString("status"), actualHold.getString("status"));
      assertEquals(expectedHold.getString("expirationDate") == null ? null : new DateTime(expectedHold.getString("expirationDate"), DateTimeZone.UTC),
          actualHold.getString("expirationDate") == null ? null : new DateTime(actualHold.getString("expirationDate"), DateTimeZone.UTC));
      assertEquals(expectedHold.getInteger("requestPosition"),
          actualHold.getInteger("requestPosition"));
      assertEquals(expectedHold.getString("patronComments"),
          actualHold.getString("patronComments"));
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
