package org.folio.rest.impl;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PatronUtils {
  private static final Logger log = LogManager.getLogger();

  private static final String SECURE_TENANT_ID = "SECURE_TENANT_ID";
  private static final String OKAPI_TENANT = "x-okapi-tenant";

  public static boolean isTenantSecure(Map<String, String> okapiHeaders) {
    String secureTenantId = System.getenv().getOrDefault(SECURE_TENANT_ID, EMPTY);
    log.info("isTenantSecure:: SECURE_TENANT_ID: {}", secureTenantId);
    String tenantFromHeaders = okapiHeaders.get(OKAPI_TENANT);
    log.info("isTenantSecure:: tenantFromHeaders: {}", tenantFromHeaders);

    return secureTenantId.equals(tenantFromHeaders);
  }
}
