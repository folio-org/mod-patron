package org.folio.rest.impl;

public enum UrlPath {
  CIRCULATION_REQUESTS_ALLOWED_SERVICE_POINTS_URL_PATH("/circulation/requests/allowed-service-points"),
  CIRCULATION_BFF_ALLOWED_SERVICE_POINTS_URL_PATH("/circulation-bff/allowed-service-points"),
  CIRCULATION_SETTINGS_STORAGE_URL_PATH("/circulation-settings-storage/circulation-settings"),
  ECS_TLR_SETTINGS_URL_PATH("/tlr/settings");

  UrlPath(String value) {
    this.value = value;
  }

  private final String value;

  public String getValue() {
    return value;
  }
}
