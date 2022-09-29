package org.folio.integration.http;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class VertxOkapiHttpClient {
  private final WebClient client;
  private static final Logger logger = LogManager.getLogger("okapi");
  

  public VertxOkapiHttpClient(WebClient client) {
    this.client = client;
  }

  public CompletableFuture<Response> get(String path, Map<String, String> okapiHeaders) {
    return get(path, Map.of(), okapiHeaders);
  }

  public CompletableFuture<Response> get(String path,
    Map<String, String> queryParameters, Map<String, String> okapiHeaders) {

    URL url = buildUrl(path, okapiHeaders);

    final var request = client
      .get(url.getPort(), url.getHost(), url.getPath())
      .putHeaders(buildHeaders(okapiHeaders))
      .timeout(5000);

    queryParameters.forEach(request::addQueryParam);

    return request.send()
      .onFailure(err -> {
        String errorMsg = "Error when trying to retrive data from " +
          url.getPath() + " " + err.getMessage();
        logger.error(errorMsg);
      })
      .toCompletionStage()
      .toCompletableFuture()
      .thenApply(this::toResponse);
  }

  public CompletableFuture<Response> post(String path, JsonObject body,
    Map<String, String> okapiHeaders) {

    URL url = buildUrl(path, okapiHeaders);

    final var request = client
      .post(url.getPort(), url.getHost(), url.getPath())
      .putHeaders(buildHeaders(okapiHeaders))
      .timeout(5000);

    return makeRequestWithBody(request, body, url.getPath());
  }

  public CompletableFuture<Response> put(String path, JsonObject body,
    Map<String, String> okapiHeaders) {

    URL url = buildUrl(path, okapiHeaders);

    final var request = client
      .put(url.getPort(), url.getHost(), url.getPath())
      .putHeaders(buildHeaders(okapiHeaders));

    return makeRequestWithBody(request, body, url.getPath());
  }

  private CompletableFuture<Response> makeRequestWithBody(
    HttpRequest<Buffer> request, JsonObject body, String path) {

    return request.sendJson(body)
      .onFailure(err -> {
        String errorMsg = "Error when trying to retrive data from " +
          path + " " + err.getMessage();
        logger.error(errorMsg);
      })
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
