package org.folio.integration.http;

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.folio.HttpStatus.HTTP_CREATED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;

class VertxOkapiHttpClientTest {
  private final WireMockServer fakeWebServer = new WireMockServer();
  private Vertx vertx;

  @BeforeEach
  void beforeEach() {
    fakeWebServer.start();

    vertx = Vertx.vertx();
  }

  @SneakyThrows
  @AfterEach
  void afterEach() {
    fakeWebServer.stop();

    stopVertx();
  }

  @SneakyThrows
  @Test
  public void canPostWithJson() {
    final String locationResponseHeader = "/a-different-location";

    fakeWebServer.stubFor(matchingFolioHeaders(post(urlPathEqualTo("/record")))
      .withHeader("Content-Type", equalTo("application/json"))
      .withRequestBody(equalToJson(dummyJsonRequestBody().encodePrettily()))
      .willReturn(created().withBody(dummyJsonResponseBody())
        .withHeader("Content-Type", "application/json")
        .withHeader("Location", locationResponseHeader)));

    final var client = createClient();

    final var postCompleted = client.post(
      "/record", dummyJsonRequestBody(), Headers.toMap(fakeWebServer.baseUrl()));

    final var response = postCompleted.get(2, SECONDS);

    assertThat(response.statusCode, is(HTTP_CREATED.toInt()));
    assertThat(asJson(response.body).getString("message"), is("hello"));
  }

  private MappingBuilder matchingFolioHeaders(MappingBuilder mappingBuilder) {
    return mappingBuilder
      .withHeader("X-Okapi-Url", equalTo(fakeWebServer.baseUrl()))
      .withHeader("X-Okapi-Tenant", equalTo(Headers.tenantId))
      .withHeader("X-Okapi-Token", equalTo(Headers.token))
      .withHeader("X-Okapi-User-Id", equalTo(Headers.userId))
      .withHeader("X-Okapi-Request-Id", equalTo(Headers.requestId));
  }

  private VertxOkapiHttpClient createClient() {
    return new VertxOkapiHttpClient(vertx);
  }

  private JsonObject dummyJsonRequestBody() {
    return new JsonObject().put("from", "James");
  }

  private String dummyJsonResponseBody() {
    return new JsonObject().put("message", "hello")
      .encodePrettily();
  }

  private JsonObject asJson(String body) {
    return new JsonObject(body);
  }

  private void stopVertx() throws InterruptedException, ExecutionException, TimeoutException {
    final var closeFuture = vertx.close();

    closeFuture.toCompletionStage().toCompletableFuture().get(1, SECONDS);
  }

  private static class Headers {
    private static final String tenantId = "test-tenant";
    private static final String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6ImFhMjZjYjg4LTc2YjEtNTQ1OS1hMjM1LWZjYTRmZDI3MGMyMyIsImlhdCI6MTU3NjAxMzY3MiwidGVuYW50IjoiZGlrdSJ9.oGCb0gDIdkXGlCiECvJHgQMXD3QKKW2vTh7PPCrpds8";
    private static final String userId = "aa26cb88-76b1-5459-a235-fca4fd270c23";
    private static final String requestId = "test-request-id";

    static Map<String, String> toMap(String okapiUrl) {
      return Map.of(
        "X-Okapi-Url", okapiUrl,
        "X-Okapi-Tenant", Headers.tenantId,
        "X-Okapi-Token", Headers.token,
        "X-Okapi-User-Id", Headers.userId,
        "X-Okapi-Request-Id", Headers.requestId);
    }
  }
}
