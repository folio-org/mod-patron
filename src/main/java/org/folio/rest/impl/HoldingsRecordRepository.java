package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.integration.http.ResponseInterpreter;
import org.folio.integration.http.VertxOkapiHttpClient;

import io.vertx.core.json.JsonObject;

public class HoldingsRecordRepository {
  private final VertxOkapiHttpClient client;

  public HoldingsRecordRepository(VertxOkapiHttpClient client) {
    this.client = client;
  }

  public CompletableFuture<JsonObject> getHoldingsRecord(String holdingsRecordId,
    Map<String, String> okapiHeaders) {
    return client.get("/holdings-storage/holdings/" + holdingsRecordId, Map.of(), okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody);
  }
}
