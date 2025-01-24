package org.folio.rest.impl;

import static org.folio.rest.impl.UrlPath.CIRCULATION_BFF_CREATE_ECS_REQUEST_EXTERNAL;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.integration.http.Response;
import org.folio.integration.http.VertxOkapiHttpClient;

import io.vertx.core.json.JsonObject;

public class CirculationRequestService {
  private static final Logger log = LogManager.getLogger();

  private CirculationRequestService() {
  }

  public static CompletableFuture<Response> createRequest(JsonObject hold,
    VertxOkapiHttpClient httpClient, Map<String, String> okapiHeaders) {

    return httpClient.postExtendedTimeout(CIRCULATION_BFF_CREATE_ECS_REQUEST_EXTERNAL, hold, okapiHeaders);
  }

}
