package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.integration.http.ResponseInterpreter;
import org.folio.integration.http.VertxOkapiHttpClient;

import io.vertx.core.json.JsonObject;
import org.folio.patron.rest.models.User;

public class UserRepository {
  private static final Logger logger = LogManager.getLogger();
  private final VertxOkapiHttpClient client;
  private static final String QUERY = "query";
  private static final String USERS = "/users";
  public UserRepository(VertxOkapiHttpClient client) {
    this.client = client;
  }

  public CompletableFuture<JsonObject> getUser(String userId, Map<String, String> okapiHeaders) {
    logger.info("getUser::Retrieving user with ID: {}", userId);
    return client.get("/users/" + userId, Map.of(), okapiHeaders)
      .thenApply(response -> {
        logger.info("getUser::Successfully retrieved user with ID: {}", userId);
        return ResponseInterpreter.verifyAndExtractBody(response);
      });
  }

  public CompletableFuture<JsonObject> getUserByCql(String cqlQuery, Map<String, String> okapiHeaders) {
    logger.info("getUserByCql::Retrieving user by cqlQuery {}", cqlQuery);
    Map<String, String> queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QUERY, cqlQuery);
    return client.get(USERS, queryParameters, okapiHeaders)
      .thenApply(response -> {
        logger.info("getUserByCql::Successfully retrieved user by query: {}", cqlQuery);
        return ResponseInterpreter.verifyAndExtractBody(response);
      });
  }

  public CompletableFuture<JsonObject> createUser(User user, Map<String, String> okapiHeaders) {
    logger.info("createUser::Creating user: {}", user);
    return client.post(USERS, JsonObject.mapFrom(user), okapiHeaders)
      .thenApply(response -> {
        logger.info("createUser::Successfully created user: {}", user);
        return ResponseInterpreter.verifyAndExtractBody(response);
      });
  }

  public CompletableFuture<JsonObject> updateUser(String id, User user, Map<String, String> okapiHeaders) {
    logger.info("updateUser::Updating user with ID: {}", id);
    return client.put("/users/" + id, JsonObject.mapFrom(user), okapiHeaders)
      .thenApply(response -> {
        logger.info("updateUser::Successfully updated user with ID: {}", id);
        return ResponseInterpreter.verifyAndExtractBody(response);
      });
  }

  public CompletableFuture<JsonObject> getGroupByGroupName(String groupName, Map<String, String> okapiHeaders) {
    logger.info("getGroupByGroupName::Retrieving group by name: {}", groupName);
    Map<String, String> queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QUERY, "(group==" + groupName + ")");
    return client.get("/groups", queryParameters, okapiHeaders)
      .thenApply(response -> {
        logger.info("getGroupByGroupName::Successfully retrieved group by name: {}", groupName);
        return ResponseInterpreter.verifyAndExtractBody(response);
      });
  }

  public CompletableFuture<JsonObject> getUsersByExpDate(String date, Map<String, String> okapiHeaders) {
    logger.info("getUsers::Retrieving users");
    Map<String, String> queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QUERY, "(expirationDate="+date+" AND type==patron)");
    queryParameters.put("limit", "100000");
    return client.get(USERS, queryParameters, okapiHeaders)
      .thenApply(response -> {
        logger.info("getUsers::Successfully retrieved users");
        return ResponseInterpreter.verifyAndExtractBody(response);
      });
  }

  public CompletableFuture<JsonObject> getAddressByType(Map<String, String> okapiHeaders) {
    logger.info("getAddressByType::Retrieving address types");
    Map<String, String> queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QUERY, "(addressType=home)");
    return client.get("/addresstypes", queryParameters, okapiHeaders)
      .thenApply(response -> {
        logger.info("getAddressByType::Successfully retrieved address types");
        return ResponseInterpreter.verifyAndExtractBody(response);
      });
  }
}
