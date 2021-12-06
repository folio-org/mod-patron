package org.folio.rest.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.integration.http.ResponseInterpreter;
import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.patron.rest.exceptions.ValidationException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Parameter;

import io.vertx.core.json.JsonObject;

public class HoldingsRecordRepository {
  private final VertxOkapiHttpClient client;

  public HoldingsRecordRepository(VertxOkapiHttpClient client) {
    this.client = client;
  }

  public CompletableFuture<JsonObject> getHoldingsRecord(JsonObject item,
    Map<String, String> okapiHeaders) throws ValidationException {

    String holdingsRecordId = item.getString("holdingsRecordId");
    if (holdingsRecordId == null){
      throw new ValidationException(new Errors()
        .withErrors(
          List.of(new Error().withMessage("HoldingsRecordId for this item is null")
            .withParameters(Collections.singletonList(
              new Parameter().withKey(Constants.JSON_FIELD_ITEM_ID)
                .withValue(item.getString("id"))
            )))));
    }

    return client.get("/holdings-storage/holdings/" + holdingsRecordId, Map.of(), okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody);
  }
}
