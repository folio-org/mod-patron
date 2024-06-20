package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Maps;
import org.folio.integration.http.ResponseInterpreter;
import org.folio.integration.http.VertxOkapiHttpClient;

import io.vertx.core.json.JsonObject;
import org.folio.patron.rest.models.User;

public class UserRepository {
  private final VertxOkapiHttpClient client;
  private static final String QUERY = "query";
  public UserRepository(VertxOkapiHttpClient client) {
    this.client = client;
  }

  public CompletableFuture<JsonObject> getUser(String userId, Map<String, String> okapiHeaders) {
    return client.get("/users/" + userId, Map.of(), okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody);
  }
  public CompletableFuture<JsonObject> getUserByEmail(String email, Map<String, String> okapiHeaders) {
    Map<String, String> queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QUERY, "(personal.email=="+email+")");
    return client.get("/users", queryParameters, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody);
  }

  public CompletableFuture<JsonObject> createUser(User user, Map<String, String> okapiHeaders) {
    return client.post("/users", JsonObject.mapFrom(user), okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody);
  }

  public CompletableFuture<JsonObject> getGroupByGroupName(String groupName, Map<String, String> okapiHeaders) {
    Map<String, String> queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QUERY, "(group=="+groupName+")");
    return client.get("/groups", queryParameters, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody);
  }

  public CompletableFuture<JsonObject> getUsers(String patronGroup, Map<String, String> okapiHeaders) {
    Map<String, String> queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QUERY, "patronGroup="+patronGroup);
    queryParameters.put("limit", "1000");
    return client.get("/users", queryParameters, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody);
  }

  public CompletableFuture<JsonObject> getAddressByType(Map<String, String> okapiHeaders) {
    Map<String, String> queryParameters = Maps.newLinkedHashMap();
    queryParameters.put(QUERY, "(addressType=work OR addressType=home)");
    return client.get("/addresstypes", queryParameters, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody);
  }
}
