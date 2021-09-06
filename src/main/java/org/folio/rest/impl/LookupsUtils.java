package org.folio.rest.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.folio.integration.http.Response;
import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.patron.rest.exceptions.HttpException;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

class LookupsUtils {
  private LookupsUtils() {}

  static CompletableFuture<JsonObject> getItem(String itemId, Map<String, String> okapiHeaders) {
    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    return client.get("/inventory/items/" + itemId, Map.of(), okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  static CompletableFuture<JsonObject> getUser(String userId, Map<String, String> okapiHeaders) {
    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    return client.get("/users/" + userId, Map.of(), okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  static CompletableFuture<JsonObject> getRequestPolicyId(RequestTypeParameters criteria, Map<String, String> okapiHeaders) {
    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    final var queryParameters = Map.of(
      "item_type_id", criteria.getItemMaterialTypeId(),
      "loan_type_id", criteria.getItemLoanTypeId(),
      "patron_type_id", criteria.getPatronGroupId(),
      "location_id", criteria.getItemLocationId());

    return client.get("/circulation/rules/request-policy", queryParameters, okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  static CompletableFuture<JsonObject> getRequestPolicy(String requestPolicyId, Map<String, String> okapiHeaders) {
    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    return client.get("/request-policy-storage/request-policies/" + requestPolicyId,
        Map.of(), okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  static JsonObject verifyAndExtractBody(Response response) {
    if (!response.isSuccess()) {
      throw new CompletionException(new HttpException(response.statusCode,
        response.body));
    }

    // Parsing an empty body to JSON causes an exception
    if (isBlank(response.body)) {
      return null;
    }
    return new JsonObject(response.body);
  }
}
