package org.folio.service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.patron.rest.exceptions.PatronSettingsException;
import org.folio.rest.jaxrs.model.Setting;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.interfaces.Results;

@Slf4j
@RequiredArgsConstructor
public class PatronSettingsService {

  public static final String MULTI_ITEM_REQUESTING_FEATURE_SETTING_KEY = "isMultiItemRequestingFeatureEnabled";

  public static final String SETTINGS_TABLE = "settings";

  private final PostgresClient postgresClient;

  public CompletableFuture<Boolean> isMultiItemRequestingFeatureEnabled() {
    var searchCriteria = new Criteria().addField("'key'").setOperation("=").setVal(MULTI_ITEM_REQUESTING_FEATURE_SETTING_KEY);
    var criterion = new Criterion().addCriterion(searchCriteria).setLimit(new Limit(1));

    return postgresClient.get(SETTINGS_TABLE, Setting.class, criterion)
      .map(results -> getFirstExceptionally(MULTI_ITEM_REQUESTING_FEATURE_SETTING_KEY, results))
      .map(Setting::getValue)
      .map(value -> Optional.ofNullable(value)
        .map(Object::toString)
        .map(Boolean::valueOf)
        .orElseThrow(() -> PatronSettingsException.invalidSettingValue(MULTI_ITEM_REQUESTING_FEATURE_SETTING_KEY, value)))
      .onSuccess(enabled -> log.info("isMultiItemRequestingFeatureEnabled:: found value for setting[key={}]={}",
        MULTI_ITEM_REQUESTING_FEATURE_SETTING_KEY, enabled))
      .onFailure(err -> log.error("isMultiItemRequestingFeatureEnabled:: failed", err))
      .toCompletionStage().toCompletableFuture();
  }

  private Setting getFirstExceptionally(String key, Results<Setting> results) {
    if (CollectionUtils.isEmpty(results.getResults())) {
      throw PatronSettingsException.notFoundByKey(key);
    }

    return results.getResults().getFirst();
  }
}
