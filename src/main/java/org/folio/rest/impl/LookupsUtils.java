package org.folio.rest.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.concurrent.CompletionException;

import org.folio.integration.http.Response;
import org.folio.patron.rest.exceptions.HttpException;

import io.vertx.core.json.JsonObject;

class LookupsUtils {
  private LookupsUtils() {}

  static JsonObject verifyAndExtractBody(Response response) {
    if (!response.isSuccess()) {
      throw new CompletionException(new HttpException(response.statusCode,
        response.body));
    }

    // Parsing an empty body to JSON causes an exception
    if (isBlank(response.body)) {
      return null;
    }
    return new JsonObject(response.body);
  }
}
