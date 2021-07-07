package org.folio.integration.http;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.vertx.core.AsyncResult;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class VertxOkapiHttpClient {
  private final Vertx vertx;

  public VertxOkapiHttpClient(Vertx vertx) {
    this.vertx = vertx;
  }

  public CompletableFuture<Response> post(String path, JsonObject body,
    Map<String, String> okapiHeaders) {

    URL url = buildUrl(path, okapiHeaders);

    final var futureResponse
      = new CompletableFuture<AsyncResult<HttpResponse<Buffer>>>();

    final var request = WebClient.create(vertx)
      .post(url.getPort(), url.getHost(), url.getPath())
      .putHeaders(buildHeaders(okapiHeaders));

    request.sendJson(body, futureResponse::complete);

    return futureResponse
      .thenCompose(this::toResponse);
  }

  public CompletableFuture<Response> put(String path, JsonObject body,
    Map<String, String> okapiHeaders) {

    URL url = buildUrl(path, okapiHeaders);

    final var futureResponse
      = new CompletableFuture<AsyncResult<HttpResponse<Buffer>>>();

    final var request = WebClient.create(vertx)
      .put(url.getPort(), url.getHost(), url.getPath())
      .putHeaders(buildHeaders(okapiHeaders));

    request.sendJson(body, futureResponse::complete);

    return futureResponse
      .thenCompose(this::toResponse);
  }

  private URL buildUrl(String path, Map<String, String> okapiHeaders) {
    try {
      final var okapiURL = okapiHeaders.getOrDefault("X-Okapi-Url", "");

      return new URL(okapiURL + path);

    } catch (MalformedURLException e) {
      throw new CompletionException(e.getCause());
    }
  }

  private static MultiMap buildHeaders(Map<String, String> okapiHeaders) {
    return MultiMap.caseInsensitiveMultiMap()
      .addAll(okapiHeaders);
  }

  private CompletableFuture<Response> toResponse(
    AsyncResult<HttpResponse<Buffer>> result) {

    if (result.failed()) {
      return CompletableFuture.failedFuture(result.cause());
    }

    final var response = result.result();

    final var mappedResponse = new Response(response.statusCode(),
      response.bodyAsString());

    return CompletableFuture.completedFuture(mappedResponse);
  }
}
