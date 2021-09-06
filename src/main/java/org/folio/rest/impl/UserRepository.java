package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.integration.http.VertxOkapiHttpClient;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class UserRepository {
  private final VertxOkapiHttpClient client;

  public UserRepository() {
    client = new VertxOkapiHttpClient(Vertx.currentContext().owner());
  }

  public CompletableFuture<JsonObject> getUser(String userId, Map<String, String> okapiHeaders) {
    return client.get("/users/" + userId, Map.of(), okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }
}
