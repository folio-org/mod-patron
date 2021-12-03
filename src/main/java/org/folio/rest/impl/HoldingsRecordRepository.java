package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.integration.http.ResponseInterpreter;
import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.patron.rest.exceptions.ValidationException;


import io.vertx.core.json.JsonObject;

public class HoldingsRecordRepository {
  private final VertxOkapiHttpClient client;

  public HoldingsRecordRepository(VertxOkapiHttpClient client) {
    this.client = client;
  }

  public CompletableFuture<JsonObject> getHoldingsRecord(JsonObject item,
    Map<String, String> okapiHeaders) throws ValidationException {

    String holdingsRecordId = item.getString("holdingsRecordId");
    if (holdingsRecordId == null) {
      throw new ValidationException("HoldingsRecordId for this item is null");
    }

    return client.get("/holdings-storage/holdings/" + holdingsRecordId, Map.of(), okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody);
  }
}
