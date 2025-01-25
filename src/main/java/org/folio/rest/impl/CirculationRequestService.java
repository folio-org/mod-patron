package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.integration.http.Response;
import org.folio.integration.http.VertxOkapiHttpClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.folio.rest.impl.Constants.JSON_FIELD_FULFILLMENT_PREFERENCE;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_LEVEL;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_TYPE;
import static org.folio.rest.impl.Constants.JSON_VALUE_HOLD_SHELF;
import static org.folio.rest.impl.PatronUtils.isTenantSecure;

public class CirculationRequestService {
  private static final Logger log = LogManager.getLogger();
  private CirculationRequestService() {
  }

  public static CompletableFuture<Response> createItemLevelRequest(
    boolean isEcsTlrFeatureEnabled, JsonObject hold,
    VertxOkapiHttpClient httpClient, Map<String, String> okapiHeaders) {

    log.info("createItemLevelRequest:: parameters isEcsTlrFeatureEnabled: {}, hold: {}",
      isEcsTlrFeatureEnabled, hold);

    return httpClient.postExtendedTimeout(defineUrlForRequest(isEcsTlrFeatureEnabled,
      isTenantSecure(okapiHeaders), false), hold, okapiHeaders);
  }

  public static CompletableFuture<Response> createTitleLevelRequest(
    boolean isEcsTlrFeatureEnabled, JsonObject hold,
    VertxOkapiHttpClient httpClient, Map<String, String> okapiHeaders) {

    log.info("createTitleLevelRequest:: parameters isEcsTlrFeatureEnabled: {}, hold: {}",
      isEcsTlrFeatureEnabled, hold);

    if (isEcsTlrFeatureEnabled) {
      hold
        .put(JSON_FIELD_REQUEST_LEVEL, "Title")
        .put(JSON_FIELD_REQUEST_TYPE, "Page")
        .put(JSON_FIELD_FULFILLMENT_PREFERENCE, JSON_VALUE_HOLD_SHELF);
    }

    return httpClient.postExtendedTimeout(defineUrlForRequest(isEcsTlrFeatureEnabled,
      isTenantSecure(okapiHeaders), true), hold, okapiHeaders);
  }

  private static String defineUrlForRequest(boolean isEcsTlrFeatureEnabled,
    boolean isTenantSecure, boolean isTitleLevel) {

    log.info("defineUrlForRequest:: parameters isEcsTlrFeatureEnabled: {}, " +
      "isModuleSecure: {}, isTitleLevel: {}", isEcsTlrFeatureEnabled, isTenantSecure,
      isTitleLevel);

    if (isEcsTlrFeatureEnabled) {
      String url = isTenantSecure
        ? UrlPath.CREATE_MEDIATED_REQUEST_URL
        : UrlPath.CIRCULATION_BFF_CREATE_ECS_REQUEST_EXTERNAL;

      log.info("defineUrlForRequest:: ECS request feature is enabled. URL is {}", url);

      return url;
    }

    String url = isTitleLevel
      ? UrlPath.URL_CIRCULATION_CREATE_INSTANCE_REQUEST
      : UrlPath.URL_CIRCULATION_CREATE_ITEM_REQUEST;

    log.info("defineUrlForRequest:: ECS request feature is disabled. URL is {}", url);

    return url;
  }
}
