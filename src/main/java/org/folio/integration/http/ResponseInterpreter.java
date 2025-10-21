package org.folio.integration.http;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.concurrent.CompletionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.patron.rest.exceptions.HttpException;

import io.vertx.core.json.JsonObject;

public class ResponseInterpreter {
  private static final Logger log = LogManager.getLogger();
  private ResponseInterpreter() {}

  public static JsonObject verifyAndExtractBody(Response response) {
    return extractBody(response, true);
  }

  public static JsonObject verifyAndExtractBodyNoThrow(Response response) {
    return extractBody(response, false);
  }

  private static JsonObject extractBody(Response response, boolean throwOnError) {
    log.info("extractBody:: statusCode: {}", response.statusCode);
    if (!response.isSuccess()) {
      if (throwOnError) {
        throw new CompletionException(new HttpException(response.statusCode, response.body));
      } else {
        log.error("extractBody:: response is not successful. statusCode: {}, body: {}",
          response.statusCode, response.body);
      }
    }
    if (isBlank(response.body)) {
      log.info("extractBody:: response is empty");
      return null;
    }
    return new JsonObject(response.body);
  }
}
