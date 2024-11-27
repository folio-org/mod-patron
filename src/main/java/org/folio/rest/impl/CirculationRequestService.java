package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;
import org.folio.integration.http.Response;
import org.folio.integration.http.VertxOkapiHttpClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.folio.rest.impl.Constants.JSON_FIELD_FULFILLMENT_PREFERENCE;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_LEVEL;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_TYPE;
import static org.folio.rest.impl.Constants.JSON_VALUE_HOLD_SHELF;

public class CirculationRequestService {

  private CirculationRequestService() {
  }

  public static CompletableFuture<Response> createItemLevelRequest(
    boolean isEcsTlrFeatureEnabled, JsonObject hold,
    VertxOkapiHttpClient httpClient, Map<String, String> okapiHeaders) {

    String url = isEcsTlrFeatureEnabled
      ? UrlPath.URL_CIRCULATION_BFF_CREATE_REQUEST
      : UrlPath.URL_CIRCULATION_CREATE_ITEM_REQUEST;
    return httpClient.post(url, hold, okapiHeaders);
  }

  public static CompletableFuture<Response> createTitleLevelRequest(
    boolean isEcsTlrFeatureEnabled, JsonObject hold,
    VertxOkapiHttpClient httpClient, Map<String, String> okapiHeaders) {

    if (isEcsTlrFeatureEnabled) {
      hold
        .put(JSON_FIELD_REQUEST_LEVEL, "Title")
        .put(JSON_FIELD_REQUEST_TYPE, "Page")
        .put(JSON_FIELD_FULFILLMENT_PREFERENCE, JSON_VALUE_HOLD_SHELF);
    }

    String url = isEcsTlrFeatureEnabled
      ? UrlPath.CIRCULATION_BFF_CREATE_ECS_REQUEST_EXTERNAL
      : UrlPath.URL_CIRCULATION_CREATE_INSTANCE_REQUEST;
    return httpClient.post(url, hold, okapiHeaders);
  }

}
