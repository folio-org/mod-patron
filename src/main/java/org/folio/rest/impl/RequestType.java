package org.folio.rest.impl;

import java.util.Arrays;

public enum RequestType {
  NONE(""),
  HOLD("Hold"),
  RECALL("Recall"),
  PAGE("Page");

  public final String value;

  public static RequestType from(String value) {
    return Arrays.stream(values())
      .filter(status -> status.nameMatches(value))
      .findFirst()
      .orElse(NONE);
  }

  RequestType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public boolean nameMatches(String value) {
    return this.value.equalsIgnoreCase(value);
  }
}
