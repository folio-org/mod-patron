package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.integration.http.ResponseInterpreter;
import org.folio.integration.http.VertxOkapiHttpClient;

import io.vertx.core.json.JsonObject;

public class UserRepository {
  private final VertxOkapiHttpClient client;

  public UserRepository(VertxOkapiHttpClient client) {
    this.client = client;
  }

  public CompletableFuture<JsonObject> getUser(String userId, Map<String, String> okapiHeaders) {
    return client.get("/users/" + userId, Map.of(), okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody);
  }
}
