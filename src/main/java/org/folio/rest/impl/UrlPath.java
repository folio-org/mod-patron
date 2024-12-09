package org.folio.rest.impl;

public class UrlPath {
  public static final String CIRCULATION_REQUESTS_ALLOWED_SERVICE_POINTS_URL_PATH =
    "/circulation/requests/allowed-service-points";
  public static final String CIRCULATION_BFF_ALLOWED_SERVICE_POINTS_URL_PATH =
    "/circulation-bff/requests/allowed-service-points";
  public static final String URL_CIRCULATION_CREATE_ITEM_REQUEST =
    "/circulation/requests";
  public static final String URL_CIRCULATION_CREATE_INSTANCE_REQUEST =
    "/circulation/requests/instances";
  public static final String CIRCULATION_SETTINGS_STORAGE_URL_PATH =
    "/circulation-settings-storage/circulation-settings";
  public static final String ECS_TLR_SETTINGS_URL_PATH ="/tlr/settings";
  public static final String CREATE_MEDIATED_REQUEST_URL =
    "/requests-mediated/mediated-requests";
  public static final String CIRCULATION_BFF_CREATE_ECS_REQUEST_EXTERNAL =
    "/circulation-bff/create-ecs-request-external";

  private UrlPath() {}
}
