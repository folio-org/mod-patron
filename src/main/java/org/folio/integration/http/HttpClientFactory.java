package org.folio.integration.http;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

/**
 * This factory allows for a single WebClient for each instance of Vert.x
 * Typically there should only be one of these in the production use of the code
 * as there is only one instance of vert.x
 * Unfortunately, the existing tests rely on creating and disposing of vert.x
 * during each test which leads to the underlying vert.x instance being
 * taken away underneath the production code.
 * Meaning that if a single WebClient (dependent upon the underlying vert.x instance)
 * is used, it breaks on all but the first test.
 * Until those tests are changed this code is needed,
 * even if it is potentially unnecessary in many use cases.
 */
public class HttpClientFactory {
  private static final ConcurrentMap<Vertx, VertxOkapiHttpClient> clientMap = new ConcurrentHashMap<>();

  private HttpClientFactory() { }

  public static VertxOkapiHttpClient getHttpClient(Vertx vertx) {
    clientMap.computeIfAbsent(vertx, HttpClientFactory::createClient);

    return clientMap.get(vertx);
  }

  private static VertxOkapiHttpClient createClient(Vertx v) {
    return new VertxOkapiHttpClient(WebClient.create(v));
  }
}
