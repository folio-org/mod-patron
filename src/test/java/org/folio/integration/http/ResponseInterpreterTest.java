package org.folio.integration.http;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class ResponseInterpreterTest {

  @Test
  void testExtractBodyReturnsNullOnErrorWhenThrowOnErrorIsFalse() {
    Response response = new Response(404, "Not Found");
    JsonObject result = ResponseInterpreter.extractBody(response, false);
    assertNull(result, "extractBody should return null for non-successful response when throwOnError is false");
  }
}

