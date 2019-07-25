package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;
import org.folio.patron.rest.exceptions.HttpException;
import org.folio.rest.RestVerticle;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

class LookupsUtils {

  private LookupsUtils(){}

  static CompletableFuture<JsonObject> getItem(String itemId, Map<String, String> okapiHeaders,
                                               HttpClientInterface httpClient) {
    return get("/inventory/items/" + itemId, httpClient, okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  static CompletableFuture<JsonObject> getUser(String userId, Map<String, String> okapiHeaders,
                                               HttpClientInterface httpClient) {
    return get("/users/" + userId, httpClient, okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  static CompletableFuture<JsonObject> getRequestPolicyId(String queryString, Map<String, String> okapiHeaders,
                                                          HttpClientInterface httpClient) {
    return get("/circulation/rules/request-policy?" + queryString, httpClient, okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  static CompletableFuture<JsonObject> getRequestPolicy(String requestPolicyId, Map<String, String> okapiHeaders,
                                                        HttpClientInterface httpClient) {
    return get("/request-policy-storage/request-policies/" + requestPolicyId, httpClient, okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  static JsonObject verifyAndExtractBody(Response response) {
    if (!Response.isSuccess(response.getCode())) {
      throw new CompletionException(new HttpException(response.getCode(),
        response.getError().getString("errorMessage")));
    }

    return response.getBody();
  }

  static HttpClientInterface getHttpClient(Map<String, String> okapiHeaders) {
    final String okapiURL = okapiHeaders.getOrDefault("X-Okapi-Url", "");
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    return HttpClientFactory.getHttpClient(okapiURL, tenantId);
  }

  private static CompletableFuture<Response> get (String path,
                                                        HttpClientInterface httpClient,
                                                        Map<String, String> okapiHeaders) {
    try {
      return httpClient.request(path, okapiHeaders);
    } catch (Exception e) {
      throw new CompletionException(e);
    }
  }
}
