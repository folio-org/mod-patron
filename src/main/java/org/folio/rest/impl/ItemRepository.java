package org.folio.rest.impl;

import static org.folio.rest.impl.Constants.JSON_FIELD_ITEM_ID;

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

public class ItemRepository {
  private final VertxOkapiHttpClient client;

  public ItemRepository(VertxOkapiHttpClient client) {
    this.client = client;
  }

  public CompletableFuture<JsonObject> getItem(String itemId,
    Map<String, String> okapiHeaders) {

    return client.get("/inventory/items/" + itemId, Map.of(), okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .thenApply(result -> {
        if (result == null) {
          throw new ValidationException(new Errors()
            .withErrors(List.of(new Error()
              .withMessage("Selected item is null in mod-inventory")
              .withParameters(Collections.singletonList(new Parameter()
                .withKey(JSON_FIELD_ITEM_ID)
                .withValue(itemId))))));
        }
        return result;
      });
  }
}
