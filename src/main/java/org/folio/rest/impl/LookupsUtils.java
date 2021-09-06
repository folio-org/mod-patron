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

  static CompletableFuture<JsonObject> getUser(String userId, Map<String, String> okapiHeaders) {
    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    return client.get("/users/" + userId, Map.of(), okapiHeaders)
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
