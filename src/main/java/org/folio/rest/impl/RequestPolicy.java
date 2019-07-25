package org.folio.rest.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class RequestPolicy {
  private final List<String> requestTypes;

  private RequestPolicy(List<String> requestTypes) {
    this.requestTypes = requestTypes;
  }

  static RequestPolicy from(JsonObject representation) {
    return new RequestPolicy(RequestPolicy
      .toStream(representation)
      .collect(Collectors.toList()));
  }

  List<RequestType> getRequestTypes() {
    return requestTypes
      .stream()
      .map(RequestType::from)
      .collect(Collectors.toList());
  }

  private static Stream<String> toStream(
    JsonObject within) {

    String arrayPropertyName = "requestTypes";

    if(within == null || !within.containsKey(arrayPropertyName)) {
      return Stream.empty();
    }

    return toStream(within.getJsonArray(arrayPropertyName));
  }

  private static Stream<String> toStream(JsonArray array) {
    return array
      .stream()
      .filter(Objects::nonNull)
      .map(castToString())
      .filter(Objects::nonNull);
  }

  private static Function<Object, String> castToString() {
    return entry -> {
      if(entry instanceof String) {
        return (String)entry;
      }
      else {
        return null;
      }
    };
  }
}
