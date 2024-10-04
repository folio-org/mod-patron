package org.folio.rest.impl;

public enum Path {

  CIRCULATION_REQUESTS_ALLOWED_SERVICE_POINTS_PATH ("/circulation/requests/allowed-service-points"),
  CIRCULATION_BFF_ALLOWED_SERVICE_POINTS_PATH("/circulation-bff/allowed-service-points"),
  CIRCULATION_SETTINGS_STORAGE_PATH("/circulation-settings-storage/circulation-settings"),
  ECS_TLR_SETTINGS_PATH("/tlr/settings");

  Path(String path) {
    this.path = path;
  }

  private final String path;

  public String getPath() {
    return path;
  }

}
