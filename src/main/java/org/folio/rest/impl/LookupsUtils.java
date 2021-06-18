package org.folio.rest.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.folio.patron.rest.exceptions.HttpException;
import org.folio.rest.RestVerticle;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;

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
    String queryString = String.format(
      "item_type_id=%s&loan_type_id=%s&patron_type_id=%s&location_id=%s",
      criteria.getItemMaterialTypeId(), criteria.getItemLoanTypeId(),
      criteria.getPatronGroupId(), criteria.getItemLocationId());

    return get("/circulation/rules/request-policy?" + queryString,
        getHttpClient(okapiHeaders), okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  static CompletableFuture<JsonObject> getRequestPolicy(String requestPolicyId, Map<String, String> okapiHeaders) {
    return get("/request-policy-storage/request-policies/" + requestPolicyId, okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  static JsonObject verifyAndExtractBody(org.folio.rest.tools.client.Response response) {
    if (!org.folio.rest.tools.client.Response.isSuccess(response.getCode())) {
      throw new CompletionException(new HttpException(response.getCode(),
        response.getError().getString("errorMessage")));
    }

    return response.getBody();
  }

  static JsonObject verifyAndExtractBody(Response response) {
    if (!response.isSuccess()) {
      throw new CompletionException(new HttpException(response.statusCode,
        response.body));
    }

    return new JsonObject(response.body);
  }

  static HttpClientInterface getHttpClient(Map<String, String> okapiHeaders) {
    final String okapiURL = okapiHeaders.getOrDefault("X-Okapi-Url", "");
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    return HttpClientFactory.getHttpClient(okapiURL, tenantId);
  }

  private static CompletableFuture<org.folio.rest.tools.client.Response> get (String path,
    HttpClientInterface httpClient, Map<String, String> okapiHeaders) {

    try {
      return httpClient.request(path, okapiHeaders);
    } catch (Exception e) {
      throw new CompletionException(e);
    }
  }

  private static CompletableFuture<LookupsUtils.Response> get (String path, Map<String, String> okapiHeaders) {
    Vertx vertx = Vertx.currentContext().owner();
    URL url;

    try {
      url = new URL(buildUri(path, okapiHeaders));
    } catch (MalformedURLException e) {
      throw new CompletionException(e.getCause());
    }

    final var futureResponse
      = new CompletableFuture<AsyncResult<HttpResponse<Buffer>>>();

    WebClient.create(vertx)
      .get(url.getPort(), url.getHost(), url.getPath())
      .putHeaders(buildHeaders(okapiHeaders))
      .send(futureResponse::complete);

    return futureResponse
      .thenCompose(LookupsUtils::toResponse);
  }

  private static CompletableFuture<LookupsUtils.Response> toResponse(
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

  public static class Response {
    public final int statusCode;
    public final String body;

    public Response(int statusCode, String body) {
      this.statusCode = statusCode;
      this.body = body;
    }

    public boolean isSuccess() {
      return statusCode >= 200 && statusCode < 300;
    }
  }
}
