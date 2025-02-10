package org.folio.rest.impl;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.impl.UrlPath.CIRCULATION_SETTINGS_STORAGE_URL_PATH;
import static org.folio.rest.impl.UrlPath.ECS_TLR_SETTINGS_URL_PATH;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.integration.http.ResponseInterpreter;
import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.patron.rest.exceptions.HttpException;
import org.folio.patron.rest.exceptions.UnexpectedFetchingException;

import io.vertx.core.json.JsonObject;

public class EcsTlrSettingsService {

  private static final Logger logger = LogManager.getLogger();
  private static final String ECS_TLR_FEATURE_KEY = "ecsTlrFeatureEnabled";
  private static final String CIRCULATION_SETTINGS_KEY = "circulationSettings";
  private static final int FIRST_POSITION_INDEX = 0;
  private static final String VALUE_KEY = "value";
  private static final String ENABLED_KEY = "enabled";

  public CompletableFuture<Boolean> isEcsTlrFeatureEnabled(VertxOkapiHttpClient httpClient,
    Map<String, String> okapiHeaders) {

    logger.info("isEcsTlrFeatureEnabled:: trying to get isEcsTlrFeatureEnabled from mod-tlr");
    return httpClient.get(ECS_TLR_SETTINGS_URL_PATH, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .exceptionally(this::handleEcsTlrSettingsFetchingError)
      .thenCompose(body -> getEcsTlrFeatureValue(body, httpClient, okapiHeaders))
      .thenApply(BooleanUtils::isTrue);
  }

  private JsonObject handleEcsTlrSettingsFetchingError(Throwable throwable) {
    if (throwable.getCause() instanceof HttpException) {
      logger.warn("handleErrorFromModTlr:: failed to fetch ECS TLR settings from mod-tlr: {}",
        throwable.getMessage());
      return null;
    }
    logger.error(throwable);

    throw new UnexpectedFetchingException(throwable);
  }

  private CompletableFuture<Boolean> getEcsTlrFeatureValue(JsonObject body,
    VertxOkapiHttpClient client, Map<String, String> okapiHeaders) {

    return Objects.nonNull(body) && body.containsKey(ECS_TLR_FEATURE_KEY)
      ? completedFuture(body.getBoolean(ECS_TLR_FEATURE_KEY))
      : getCirculationStorageEcsTlrFeatureValue(client, okapiHeaders);
  }

  private CompletableFuture<Boolean> getCirculationStorageEcsTlrFeatureValue(
    VertxOkapiHttpClient client, Map<String, String> okapiHeaders) {

    logger.info("getCirculationStorageEcsTlrFeatureValue:: trying to get isEcsTlrFeatureEnabled");
    return client.get(CIRCULATION_SETTINGS_STORAGE_URL_PATH, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .thenApply(this::getCirculationStorageEcsTlrFeatureValue);
  }

  private Boolean getCirculationStorageEcsTlrFeatureValue(JsonObject body) {
    logger.info("getCirculationStorageEcsTlrFeatureValue:: body: {}", () -> body);
    if (body.isEmpty()) {
      return false;
    }
    Boolean result = Optional.ofNullable(body)
      .map(json -> json.getJsonArray(CIRCULATION_SETTINGS_KEY))
      .map(json -> json.getJsonObject(FIRST_POSITION_INDEX))
      .map(json -> json.getJsonObject(VALUE_KEY))
      .map(json -> json.getBoolean(ENABLED_KEY))
      .orElse(false);
    logger.debug("getCirculationStorageEcsTlrFeatureValue:: result = {}", result);
    return result;
  }
}
