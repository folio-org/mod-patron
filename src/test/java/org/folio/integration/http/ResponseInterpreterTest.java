package org.folio.integration.http;

import static org.folio.integration.http.ResponseInterpreter.verifyAndExtractBodyNoThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

class ResponseInterpreterTest {

  @Test
  void testExtractBodyReturnsNullOnErrorWhenThrowOnErrorIsFalse() {
    JsonObject result = verifyAndExtractBodyNoThrow(new Response(404, "Not Found"));
    assertNull(result, "extractBody should return null for non-successful response when throwOnError is false");
  }
}

