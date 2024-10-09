package org.folio.rest.impl;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class UrlPathTest {
  private static final String CIRCULATION_REQUESTS_ALLOWED_SERVICE_POINTS_URL_PATH_EXPECTED =
    "/circulation/requests/allowed-service-points";
  private static final String CIRCULATION_BFF_ALLOWED_SERVICE_POINTS_URL_PATH_EXPECTED =
    "/circulation-bff/allowed-service-points";
  private static final String CIRCULATION_SETTINGS_STORAGE_URL_PATH_EXPECTED =
    "/circulation-settings-storage/circulation-settings";
  private static final String ECS_TLR_SETTINGS_URL_PATH_EXPECTED = "/tlr/settings";

  @Test
  void shouldReturnUrlPaths() {
    assertEquals(CIRCULATION_REQUESTS_ALLOWED_SERVICE_POINTS_URL_PATH_EXPECTED,
      UrlPath.CIRCULATION_REQUESTS_ALLOWED_SERVICE_POINTS_URL_PATH);
    assertEquals(CIRCULATION_BFF_ALLOWED_SERVICE_POINTS_URL_PATH_EXPECTED,
      UrlPath.CIRCULATION_BFF_ALLOWED_SERVICE_POINTS_URL_PATH);
    assertEquals(CIRCULATION_SETTINGS_STORAGE_URL_PATH_EXPECTED,
      UrlPath.CIRCULATION_SETTINGS_STORAGE_URL_PATH);
    assertEquals(ECS_TLR_SETTINGS_URL_PATH_EXPECTED, UrlPath.ECS_TLR_SETTINGS_URL_PATH);
  }
}
