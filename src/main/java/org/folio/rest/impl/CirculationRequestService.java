package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.integration.http.Response;
import org.folio.integration.http.VertxOkapiHttpClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.lang.System.getProperty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.rest.impl.Constants.JSON_FIELD_FULFILLMENT_PREFERENCE;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_LEVEL;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_TYPE;
import static org.folio.rest.impl.Constants.JSON_VALUE_HOLD_SHELF;

public class CirculationRequestService {
  private static final Logger log = LogManager.getLogger();
  private static final String OKAPI_TENANT = "x-okapi-tenant";
  private static final String SECURE_TENANT_ID = "SECURE_TENANT_ID";

  private CirculationRequestService() {
  }

  public static CompletableFuture<Response> createItemLevelRequest(
    boolean isEcsTlrFeatureEnabled, JsonObject hold,
    VertxOkapiHttpClient httpClient, Map<String, String> okapiHeaders) {

    log.debug("createItemLevelRequest:: parameters isEcsTlrFeatureEnabled: {}, hold: {}",
      isEcsTlrFeatureEnabled, hold);

    return httpClient.post(defineUrlForRequest(isEcsTlrFeatureEnabled,
      isTenantSecure(okapiHeaders), false), hold, okapiHeaders);
  }

  public static CompletableFuture<Response> createTitleLevelRequest(
    boolean isEcsTlrFeatureEnabled, JsonObject hold,
    VertxOkapiHttpClient httpClient, Map<String, String> okapiHeaders) {

    log.debug("createTitleLevelRequest:: parameters isEcsTlrFeatureEnabled: {}, hold: {}",
      isEcsTlrFeatureEnabled, hold);

    if (isEcsTlrFeatureEnabled) {
      hold
        .put(JSON_FIELD_REQUEST_LEVEL, "Title")
        .put(JSON_FIELD_REQUEST_TYPE, "Page")
        .put(JSON_FIELD_FULFILLMENT_PREFERENCE, JSON_VALUE_HOLD_SHELF);
    }

    return httpClient.post(defineUrlForRequest(isEcsTlrFeatureEnabled,
      isTenantSecure(okapiHeaders), true), hold, okapiHeaders);
  }

  private static String defineUrlForRequest(boolean isEcsTlrFeatureEnabled,
    boolean isModuleSecure, boolean isTitleLevel) {

    log.debug("defineUrlForRequest:: parameters isEcsTlrFeatureEnabled: {}, " +
      "isModuleSecure: {}, isTitleLevel: {}", isEcsTlrFeatureEnabled, isModuleSecure, isTitleLevel);

    if (isEcsTlrFeatureEnabled) {
      log.info("defineUrlForRequest:: ecsTlrFeature enabled");
      return isModuleSecure
        ? UrlPath.CREATE_MEDIATED_REQUEST_URL
        : UrlPath.CIRCULATION_BFF_CREATE_ECS_REQUEST_EXTERNAL;
    }

    return isTitleLevel
      ? UrlPath.URL_CIRCULATION_CREATE_INSTANCE_REQUEST
      : UrlPath.URL_CIRCULATION_CREATE_ITEM_REQUEST;
  }

  private static boolean isTenantSecure(Map<String, String> okapiHeaders) {
    String secureProperty = getProperty(SECURE_TENANT_ID, EMPTY);
    log.info("isTenantSecure:: secureProperty: {}", secureProperty);
    String tenantFromHeaders = okapiHeaders.get(OKAPI_TENANT);
    log.info("isTenantSecure:: tenantFromHeaders: {}", tenantFromHeaders);

    var result = getProperty(SECURE_TENANT_ID, EMPTY).equals(tenantFromHeaders);
    log.info("isModuleSecure:: result: {}", result);

    return result;
  }
}
