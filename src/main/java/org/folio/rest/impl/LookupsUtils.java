package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.commons.lang3.StringUtils;
import org.folio.integration.http.Response;
import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.patron.rest.exceptions.HttpException;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

class LookupsUtils {
  private LookupsUtils() {}

  static CompletableFuture<JsonObject> getItem(String itemId, Map<String, String> okapiHeaders) {
    return get("/inventory/items/" + itemId, okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  static CompletableFuture<JsonObject> getUser(String userId, Map<String, String> okapiHeaders) {
    return get("/users/" + userId, okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  static CompletableFuture<JsonObject> getRequestPolicyId(RequestTypeParameters criteria, Map<String, String> okapiHeaders) {
    final var queryParameters = Map.of(
      "item_type_id", criteria.getItemMaterialTypeId(),
      "loan_type_id", criteria.getItemLoanTypeId(),
      "patron_type_id", criteria.getPatronGroupId(),
      "location_id", criteria.getItemLocationId());

    return get("/circulation/rules/request-policy",
        queryParameters, okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  static CompletableFuture<JsonObject> getRequestPolicy(String requestPolicyId, Map<String, String> okapiHeaders) {
    return get("/request-policy-storage/request-policies/" + requestPolicyId, okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  static JsonObject verifyAndExtractBody(Response response) {
    if (!response.isSuccess()) {
      throw new CompletionException(new HttpException(response.statusCode,
        response.body));
    }

    // Parsing an emppty body to JSON causes an exception
    if (StringUtils.isBlank(response.body)) {
      return null;
    }
    return new JsonObject(response.body);
  }

  public static CompletableFuture<Response> post(String path,
    JsonObject body, Map<String, String> okapiHeaders) {

    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    return client.post(path, body, okapiHeaders);
  }

  public static CompletableFuture<Response> put(String path,
    JsonObject body, Map<String, String> okapiHeaders) {

    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    return client.put(path, body, okapiHeaders);
  }

  public static CompletableFuture<Response> get(String path,
    Map<String, String> queryParameters, Map<String, String> okapiHeaders) {

    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    return client.get(path, queryParameters, okapiHeaders);
  }

  public static CompletableFuture<Response> get(String path,
    Map<String, String> okapiHeaders) {

    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    return client.get(path, Map.of(), okapiHeaders);
  }
}
