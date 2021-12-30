package org.folio.integration.http;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.concurrent.CompletionException;

import org.folio.patron.rest.exceptions.HttpException;

import io.vertx.core.json.JsonObject;

public class ResponseInterpreter {
  private ResponseInterpreter() {}

  public static JsonObject verifyAndExtractBody(Response response) {
    System.out.println(response.body);
    System.out.println(response.statusCode);
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
