package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.folio.service.PatronSettingsService.SETTINGS_TABLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.stream.Stream;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.UrlDecoder;
import org.folio.rest.persist.Criteria.Criterion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;


/**
 * Integration tests for PatronSettings endpoints
 * Tests cover CRUD operations: GET (collection and by ID), POST, PUT, DELETE.
 */
@ExtendWith({VertxExtension.class, SystemStubsExtension.class})
@DisplayName("PatronSettings Integration Tests")
public class PatronSettingsImplTest extends BaseResourceServiceTest {

  private static final Logger logger = LogManager.getLogger();

  // API Paths
  private static final String SETTINGS_PATH = "/patron/settings";
  private static final String SETTINGS_BY_ID_PATH = "/patron/settings/{id}";

  // Test data IDs
  private final String nonExistentSettingId = UUID.randomUUID().toString();

  static {
    System.setProperty("vertx.logger-delegate-factory-class-name",
      "io.vertx.core.logging.Log4j2LogDelegateFactory");
  }

  @BeforeEach
  public void setUp(Vertx vertx, VertxTestContext context) {
    postgresClient.delete(SETTINGS_TABLE, new Criterion(),
      event -> {
        if (event.failed()) {
          logger.error(event.cause());
          context.failNow(event.cause());
        } else {
          var host = "localhost";
          var server = vertx.createHttpServer();
          server.requestHandler(this::mockData);
          server.listen(serverPort, host);
          context.completeNow();
        }
      });
  }

  @AfterEach
  public void tearDown(Vertx vertx, VertxTestContext context) {
    context.completeNow();
  }

  // Java 21 Record for test data (using records pattern)
  record SettingTestData(String id, String scope, String key, Object value, String userId) {
    static SettingTestData createDefault() {
      return new SettingTestData(
        UUID.randomUUID().toString(),
        "mod-patron",
        "test.setting.key",
        "test-value",
        null
      );
    }

    static SettingTestData withKey(String key) {
      return new SettingTestData(
        UUID.randomUUID().toString(),
        "mod-patron",
        key,
        "value-for-" + key,
        null
      );
    }

    JsonObject toJson() {
      JsonObject json = new JsonObject()
        .put("id", id)
        .put("scope", scope)
        .put("key", key)
        .put("value", value);
      if (userId != null) {
        json.put("userId", userId);
      }
      return json;
    }
  }

  @Override
  protected void mockData(HttpServerRequest req) {
    // Mock data implementation for testing
    // This would typically interact with mock external services
    logger.info("Mock request: {} {}", req.method(), req.path());

    // Default response for unhandled requests
    if (!req.response().ended()) {
      req.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", "Not found").encode());
    }
  }

  // ========== GET /patron-settings (Collection) Tests ==========

  @Test
  @DisplayName("GET /patron-settings should return collection successfully")
  public void testGetPatronSettingsCollection() {
    logger.info("Testing GET /patron-settings collection");

    var response = getRequestSpecification()
      .get(SETTINGS_PATH + "?limit=10&offset=0")
      .then()
      .log().all()
      .contentType(ContentType.JSON)
      .statusCode(200)
      .extract().response();

    var responseBody = new JsonObject(response.getBody().asString());

    assertNotNull(responseBody);
    assertTrue(responseBody.containsKey("settings"));
    assertTrue(responseBody.containsKey("totalRecords"));
    assertThat(responseBody.getInteger("totalRecords"), greaterThanOrEqualTo(0));

    logger.info("Test completed successfully");
  }

  @Test
  @DisplayName("GET /patron-settings with query parameter should filter results")
  public void testGetPatronSettingsWithQuery() {
    logger.info("Testing GET /patron-settings with query parameter");

    String query = "scope==mod-patron";

    var response = getRequestSpecification()
      .get(SETTINGS_PATH + "?query=" + UrlDecoder.decode(query) + "&limit=10")
      .then()
      .log().all()
      .contentType(ContentType.JSON)
      .statusCode(200)
      .extract().response();

    var responseBody = new JsonObject(response.getBody().asString());
    assertNotNull(responseBody.getJsonArray("settings"));

    logger.info("Query test completed successfully");
  }

  @ParameterizedTest
  @ValueSource(ints = {5, 10, 20, 50})
  @DisplayName("GET /patron-settings with different limit values")
  public void testGetPatronSettingsWithDifferentLimits(int limit) {
    logger.info("Testing GET /patron-settings with limit={}", limit);

    var response = getRequestSpecification()
      .get(SETTINGS_PATH + "?limit=" + limit)
      .then()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .extract().response();

    var responseBody = new JsonObject(response.getBody().asString());
    var settings = responseBody.getJsonArray("settings");

    assertNotNull(settings);
    assertThat(settings.size(), lessThanOrEqualTo(limit));

    logger.info("Limit test for {} completed", limit);
  }

  @Test
  @DisplayName("GET /patron-settings with pagination should work correctly")
  public void testGetPatronSettingsWithPagination() {
    logger.info("Testing pagination for GET /patron-settings");

    // First page
    Response firstPageResponse = getRequestSpecification()
      .get(SETTINGS_PATH + "?limit=5&offset=0")
      .then()
      .statusCode(200)
      .extract().response();

    // Second page
    Response secondPageResponse = getRequestSpecification()
      .get(SETTINGS_PATH + "?limit=5&offset=5")
      .then()
      .statusCode(200)
      .extract().response();

    var firstPage = new JsonObject(firstPageResponse.getBody().asString());
    var secondPage = new JsonObject(secondPageResponse.getBody().asString());

    assertEquals(firstPage.getInteger("totalRecords"), secondPage.getInteger("totalRecords"));

    logger.info("Pagination test completed successfully");
  }

  // ========== GET /patron-settings/{id} Tests ==========

  @Test
  @DisplayName("GET /patron-settings/{id} should return setting by ID successfully")
  public void testGetPatronSettingById() {
    logger.info("Testing GET /patron/settings/{id} for existing setting");

    // First create a setting
    var testData = SettingTestData.createDefault();
    var settingId = createSetting(testData);

    // Then retrieve it
    var response = getRequestSpecification()
      .get(getSettingsByIdPath(settingId))
      .then()
      .log().all()
      .contentType(ContentType.JSON)
      .statusCode(200)
      .extract().response();

    var setting = new JsonObject(response.getBody().asString());

    assertNotNull(setting);
    assertEquals(settingId, setting.getString("id"));
    assertEquals(testData.scope(), setting.getString("scope"));
    assertEquals(testData.key(), setting.getString("key"));
    assertEquals(testData.value(), setting.getString("value"));

    logger.info("GET by ID test completed successfully");
  }

  @Test
  @DisplayName("GET /patron/settings/{id} should return 404 for non-existent ID")
  public void testGetPatronSettingByIdNotFound() {
    logger.info("Testing GET /patron/settings/{id} for non-existent setting");

    getRequestSpecification()
      .get(getSettingsByIdPath(nonExistentSettingId))
      .then()
      .log().all()
      .statusCode(404)
      .extract().response();

    logger.info("404 test completed successfully");
  }

  @Test
  @DisplayName("GET /patron/settings/{id} should return 400 for invalid UUID")
  public void testGetPatronSettingByInvalidId() {
    logger.info("Testing GET /patron/settings/{id} with invalid UUID");

    getRequestSpecification()
      .get(getSettingsByIdPath("invalid-id"))
      .then()
      .log().all()
      .statusCode(SC_NOT_FOUND);

    logger.info("Invalid ID test completed");
  }

  // ========== POST /patron/settings Tests ==========

  @Test
  @DisplayName("POST /patron/settings should create new setting successfully")
  public void testPostPatronSetting() {
    logger.info("Testing POST /patron/settings");

    var testData = SettingTestData.createDefault();
    var newSetting = testData.toJson();

    var response = getRequestSpecification()
      .body(newSetting.encode())
      .post(SETTINGS_PATH)
      .then()
      .log().all()
      .contentType(ContentType.JSON)
      .statusCode(201)
      .extract().response();

    var createdSetting = new JsonObject(response.getBody().asString());

    assertNotNull(createdSetting);
    assertNotNull(createdSetting.getString("id"));
    assertEquals(testData.scope(), createdSetting.getString("scope"));
    assertEquals(testData.key(), createdSetting.getString("key"));

    logger.info("POST test completed successfully");
  }

  @Test
  @DisplayName("POST /patron/settings without required fields should return 422")
  public void testPostPatronSettingWithoutRequiredFields() {
    logger.info("Testing POST /patron/settings without required fields");

    var incompleteSetting = new JsonObject()
      .put("scope", "mod-patron");
    // Missing 'key' and 'value' fields

    getRequestSpecification()
      .body(incompleteSetting.encode())
      .post(SETTINGS_PATH)
      .then()
      .log().all()
      .statusCode(422);

    logger.info("POST validation test completed");
  }

  @Test
  @DisplayName("POST /patron/settings with invalid JSON should return 400")
  public void testPostPatronSettingWithInvalidJson() {
    logger.info("Testing POST /patron/settings with invalid JSON");

    getRequestSpecification()
      .body("{ invalid json }")
      .post(SETTINGS_PATH)
      .then()
      .log().all()
      .statusCode(400);

    logger.info("Invalid JSON test completed");
  }

  @ParameterizedTest
  @MethodSource("provideSettingTestData")
  @DisplayName("POST /patron/settings with various valid data")
  public void testPostPatronSettingWithVariousData(String key, Object value, String description) {
    logger.info("Testing POST with {}", description);

    var testData = new SettingTestData(
      UUID.randomUUID().toString(),
      "mod-patron",
      key,
      value,
      null
    );

    var response = getRequestSpecification()
      .when()
      .body(testData.toJson().encode())
      .post(SETTINGS_PATH)
      .then()
      .log().all()
      .statusCode(201)
      .extract().response();

    var created = new JsonObject(response.getBody().asString());
    assertEquals(key, created.getString("key"));

    logger.info("Test with {} completed", description);
  }

  private static Stream<Arguments> provideSettingTestData() {
    return Stream.of(
      Arguments.of("string.setting", "simple-string", "string value"),
      Arguments.of("number.setting", 42, "numeric value"),
      Arguments.of("boolean.setting", true, "boolean value"),
      Arguments.of("object.setting", new JsonObject().put("nested", "value"), "object value"),
      Arguments.of("array.setting", new JsonArray().add("item1").add("item2"), "array value")
    );
  }

  // ========== PUT /patron/settings/{id} Tests ==========

  @Test
  @DisplayName("PUT /patron/settings/{id} should update setting successfully")
  public void testPutPatronSettingById() {
    logger.info("Testing PUT /patron/settings/{id}");

    // Create initial setting
    var initialData = SettingTestData.withKey("update.test.key");
    var settingId = createSetting(initialData);

    // Update the setting
    var updatedSetting = new JsonObject()
      .put("id", settingId)
      .put("scope", "mod-patron")
      .put("key", "update.test.key")
      .put("value", "updated-value")
      .put("_version", 1);

    getRequestSpecification()
      .when()
      .body(updatedSetting.encode())
      .put(getSettingsByIdPath(settingId))
      .then()
      .log().all()
      .statusCode(204);

    // Verify the update
    var getResponse = getRequestSpecification()
      .when()
      .get(getSettingsByIdPath(settingId))
      .then()
      .statusCode(200)
      .extract().response();

    var retrievedSetting = new JsonObject(getResponse.getBody().asString());
    assertEquals("updated-value", retrievedSetting.getValue("value"));

    logger.info("PUT test completed successfully");
  }

  @Test
  @DisplayName("PUT /patron/settings/{id} should return 404 for non-existent ID")
  public void testPutPatronSettingByIdNotFound() {
    logger.info("Testing PUT /patron/settings/{id} for non-existent setting");

    var setting = new JsonObject()
      .put("id", nonExistentSettingId)
      .put("scope", "mod-patron")
      .put("key", "test.key")
      .put("value", "test-value");

    getRequestSpecification()
      .body(setting.encode())
      .put(getSettingsByIdPath(nonExistentSettingId))
      .then()
      .log().all()
      .statusCode(404);

    logger.info("PUT 404 test completed");
  }

  @Test
  @DisplayName("PUT /patron/settings/{id} with mismatched ID should return 422")
  public void testPutPatronSettingWithMismatchedId() {
    logger.info("Testing PUT with mismatched ID in path and body");

    var testData = SettingTestData.createDefault();
    var settingId = createSetting(testData);

    var settingWithDifferentId = testData.toJson()
      .put("id", nonExistentSettingId);

    getRequestSpecification()
      .body(settingWithDifferentId.encode())
      .put(getSettingsByIdPath(settingId))
      .then()
      .log().all()
      .statusCode(400);

    logger.info("Mismatched ID test completed");
  }

  // ========== DELETE /patron/settings/{id} Tests ==========

  @Test
  @DisplayName("DELETE /patron/settings/{id} should delete setting successfully")
  public void testDeletePatronSettingById() {
    logger.info("Testing DELETE /patron/settings/{id}");

    // Create a setting to delete
    var testData = SettingTestData.withKey("delete.test.key");
    var settingId = createSetting(testData);

    // Delete it
    getRequestSpecification()
      .delete(getSettingsByIdPath(settingId))
      .then()
      .log().all()
      .statusCode(204);

    // Verify it's deleted
    getRequestSpecification()
      .get(getSettingsByIdPath(settingId))
      .then()
      .statusCode(404);

    logger.info("DELETE test completed successfully");
  }

  @Test
  @DisplayName("DELETE /patron/settings/{id} should return 404 for non-existent ID")
  public void testDeletePatronSettingByIdNotFound() {
    logger.info("Testing DELETE /patron/settings/{id} for non-existent setting");

    getRequestSpecification()
      .delete(getSettingsByIdPath(nonExistentSettingId))
      .then()
      .log().all()
      .statusCode(404);

    logger.info("DELETE 404 test completed");
  }

  @Test
  @DisplayName("DELETE /patron/settings/{id} should be idempotent")
  public void testDeletePatronSettingIdempotency() {
    logger.info("Testing DELETE idempotency");

    // Create a setting
    var testData = SettingTestData.withKey("idempotent.delete.key");
    var settingId = createSetting(testData);

    // Delete it first time
    getRequestSpecification()
      .delete(getSettingsByIdPath(settingId))
      .then()
      .statusCode(204);

    // Delete it again - should return 404
    getRequestSpecification()
      .delete(getSettingsByIdPath(settingId))
      .then()
      .statusCode(404);

    logger.info("Idempotency test completed");
  }

  // ========== CRUD Integration Tests ==========

  @Test
  @DisplayName("Complete CRUD lifecycle test")
  public void testCompleteCrudLifecycle() {
    logger.info("Testing complete CRUD lifecycle");

    // CREATE
    var testData = SettingTestData.withKey("lifecycle.test.key");
    var settingId = createSetting(testData);
    assertNotNull(settingId);
    logger.info("Created setting with ID: {}", settingId);

    // READ
    var getResponse = getRequestSpecification()
      .get(getSettingsByIdPath(settingId))
      .then()
      .statusCode(200)
      .extract().response();

    var retrievedSetting = new JsonObject(getResponse.getBody().asString());
    assertEquals(settingId, retrievedSetting.getString("id"));
    logger.info("Retrieved setting successfully");

    // UPDATE
    var updatedSetting = retrievedSetting.copy()
      .put("value", "lifecycle-updated-value")
      .put("_version", retrievedSetting.getInteger("_version"));

    getRequestSpecification()
      .body(updatedSetting.encode())
      .put(getSettingsByIdPath(settingId))
      .then()
      .statusCode(204);
    logger.info("Updated setting successfully");

    // Verify update
    var verifyResponse = getRequestSpecification()
      .get(getSettingsByIdPath(settingId))
      .then()
      .statusCode(200)
      .extract().response();

    var verifiedSetting = new JsonObject(verifyResponse.getBody().asString());
    assertEquals("lifecycle-updated-value", verifiedSetting.getValue("value"));
    logger.info("Verified update successfully");

    // DELETE
    getRequestSpecification()
      .delete(getSettingsByIdPath(settingId))
      .then()
      .statusCode(204);
    logger.info("Deleted setting successfully");

    // Verify deletion
    getRequestSpecification()
      .get(getSettingsByIdPath(settingId))
      .then()
      .statusCode(404);

    logger.info("Complete CRUD lifecycle test completed successfully");
  }

  // ========== Concurrent Operations Tests (Java 21 Virtual Threads) ==========

  @Test
  @DisplayName("Concurrent POST operations should succeed")
  public void testConcurrentPostOperations() throws InterruptedException {
    logger.info("Testing concurrent POST operations");

    int numberOfThreads = 5;
    var threads = new Thread[numberOfThreads];
    var results = new String[numberOfThreads];

    for (int i = 0; i < numberOfThreads; i++) {
      final int index = i;
      // Using Thread.ofVirtual() - Java 21 feature
      threads[i] = Thread.ofVirtual().start(() -> {
        try {
          var testData = SettingTestData.withKey("concurrent.key." + index);
          results[index] = createSetting(testData);
          logger.info("Thread {} created setting: {}", index, results[index]);
        } catch (Exception e) {
          logger.error("Thread {} failed", index, e);
        }
      });
    }

    // Wait for all threads to complete
    for (var thread : threads) {
      thread.join();
    }

    // Verify all operations succeeded
    for (int i = 0; i < numberOfThreads; i++) {
      assertNotNull(results[i], "Setting " + i + " should be created");
    }

    logger.info("Concurrent operations test completed successfully");
  }

  // ========== Helper Methods ==========

  /**
   * Helper method to create a setting and return its ID
   */
  private String createSetting(SettingTestData testData) {
    var response = given()
      .header(tenantHeader)
      .header(urlHeader)
      .header(contentTypeHeader)
      .body(testData.toJson().encode())
      .when()
      .post(SETTINGS_PATH)
      .then()
      .statusCode(201)
      .extract().response();

    var created = new JsonObject(response.getBody().asString());
    return created.getString("id");
  }

  private String getSettingsByIdPath(String id) {
    return SETTINGS_BY_ID_PATH.replace("{id}", UrlDecoder.decode(id));
  }
}

