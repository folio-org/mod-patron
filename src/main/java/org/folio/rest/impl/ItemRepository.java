package org.folio.rest.impl;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.impl.Constants.JSON_FIELD_ITEM_ID;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.integration.http.Response;
import org.folio.integration.http.ResponseInterpreter;
import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.patron.rest.exceptions.ValidationException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Parameter;

import io.vertx.core.json.JsonObject;

public class ItemRepository {
  private static final Logger log = LogManager.getLogger();
  private static final String INVENTORY_ITEMS_URL = "/inventory/items/";
  private static final String CIRCULATION_ITEMS_URL = "/circulation-item/";
  private final VertxOkapiHttpClient client;

  public ItemRepository(VertxOkapiHttpClient client) {
    this.client = client;
  }

  public CompletableFuture<JsonObject> getItem(String itemId,
    Map<String, String> okapiHeaders) {

    return client.get(INVENTORY_ITEMS_URL + itemId, Map.of(), okapiHeaders)
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

  public CompletableFuture<JsonObject> getItemNoThrow(String itemId,
    Map<String, String> okapiHeaders) {

    return findItem(itemId, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBodyNoThrow);
  }

  private CompletableFuture<Response> findItem(String itemId, Map<String, String> okapiHeaders) {
    return client.get(INVENTORY_ITEMS_URL + itemId, Map.of(), okapiHeaders)
      .thenCompose(response -> {
        if (response.statusCode == HttpStatus.SC_NOT_FOUND) {
          log.info("findItem:: item was not found in inventory, looking for circulation item");
          return client.get(CIRCULATION_ITEMS_URL + itemId, Map.of(), okapiHeaders);
        }
        return completedFuture(response);
      });
  }

}
