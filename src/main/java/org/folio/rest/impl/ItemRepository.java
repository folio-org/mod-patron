package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.integration.http.VertxOkapiHttpClient;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ItemRepository {
  private final VertxOkapiHttpClient client;

  public ItemRepository() {
    client = new VertxOkapiHttpClient(Vertx.currentContext().owner());
  }

  public CompletableFuture<JsonObject> getItem(String itemId,
    Map<String, String> okapiHeaders) {

    return client.get("/inventory/items/" + itemId, Map.of(), okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }
}
