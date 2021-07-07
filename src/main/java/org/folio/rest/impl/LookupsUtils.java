package org.folio.rest.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.commons.lang3.StringUtils;
import org.folio.integration.http.Response;
import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.patron.rest.exceptions.HttpException;

import io.vertx.core.AsyncResult;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

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

    Vertx vertx = Vertx.currentContext().owner();
    URL url;

    try {
      url = new URL(buildUri(path, okapiHeaders));
    } catch (MalformedURLException e) {
      throw new CompletionException(e.getCause());
    }

    final var futureResponse
      = new CompletableFuture<AsyncResult<HttpResponse<Buffer>>>();

    final var request = WebClient.create(vertx)
      .put(url.getPort(), url.getHost(), url.getPath())
      .putHeaders(buildHeaders(okapiHeaders));

    request
      .timeout(1000)
      .sendJson(body, futureResponse::complete);

    return futureResponse
      .thenCompose(LookupsUtils::toResponse);
  }

  public static CompletableFuture<Response> get(String path,
                                                Map<String, String> queryParameters, Map<String, String> okapiHeaders) {

    Vertx vertx = Vertx.currentContext().owner();
    URL url;

    try {
      url = new URL(buildUri(path, okapiHeaders));
    } catch (MalformedURLException e) {
      throw new CompletionException(e.getCause());
    }

    final var futureResponse
      = new CompletableFuture<AsyncResult<HttpResponse<Buffer>>>();

    final var request = WebClient.create(vertx)
      .get(url.getPort(), url.getHost(), url.getPath())
      .putHeaders(buildHeaders(okapiHeaders));

    queryParameters.forEach(request::addQueryParam);

    request.send(futureResponse::complete);

    return futureResponse
      .thenCompose(LookupsUtils::toResponse);
  }

  public static CompletableFuture<Response> get(String path,
                                                Map<String, String> okapiHeaders) {

    return get(path, Map.of(), okapiHeaders);
  }

  private static CompletableFuture<Response> toResponse(
    AsyncResult<HttpResponse<Buffer>> result) {

    if (result.failed()) {
      return CompletableFuture.failedFuture(result.cause());
    }

    final var response = result.result();

    final var mappedResponse = new Response(response.statusCode(),
      response.bodyAsString());

    return CompletableFuture.completedFuture(mappedResponse);
  }

  private static String buildUri(String path, Map<String, String> okapiHeaders) {
    final String okapiURL = okapiHeaders.getOrDefault("X-Okapi-Url", "");

    return okapiURL + path;
  }

  private static MultiMap buildHeaders(Map<String, String> okapiHeaders) {
    return MultiMap.caseInsensitiveMultiMap()
      .addAll(okapiHeaders);
  }
}
