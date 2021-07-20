package org.folio.integration.http;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class VertxOkapiHttpClient {
  private final WebClient client;

  public VertxOkapiHttpClient(Vertx vertx) {
    client = WebClient.create(vertx);
  }

  public CompletableFuture<Response> get(String path, Map<String, String> okapiHeaders) {
    return get(path, Map.of(), okapiHeaders);
  }

  public CompletableFuture<Response> get(String path,
    Map<String, String> queryParameters, Map<String, String> okapiHeaders) {

    URL url = buildUrl(path, okapiHeaders);

    final var request = client
      .get(url.getPort(), url.getHost(), url.getPath())
      .putHeaders(buildHeaders(okapiHeaders));

    queryParameters.forEach(request::addQueryParam);

    return request.send()
      .toCompletionStage()
      .toCompletableFuture()
      .thenApply(this::toResponse);
  }

  public CompletableFuture<Response> post(String path, JsonObject body,
    Map<String, String> okapiHeaders) {

    URL url = buildUrl(path, okapiHeaders);

    final var request = client
      .post(url.getPort(), url.getHost(), url.getPath())
      .putHeaders(buildHeaders(okapiHeaders));

    return makeRequestWithBody(request, body);
  }

  public CompletableFuture<Response> put(String path, JsonObject body,
    Map<String, String> okapiHeaders) {

    URL url = buildUrl(path, okapiHeaders);

    final var request = client
      .put(url.getPort(), url.getHost(), url.getPath())
      .putHeaders(buildHeaders(okapiHeaders));

    return makeRequestWithBody(request, body);
  }

  private CompletableFuture<Response> makeRequestWithBody(
    HttpRequest<Buffer> request, JsonObject body) {

    return request.sendJson(body)
      .toCompletionStage()
      .toCompletableFuture()
      .thenApply(this::toResponse);
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

  private Response toResponse(HttpResponse<Buffer> response) {
    return new Response(response.statusCode(), response.bodyAsString());
  }
}
