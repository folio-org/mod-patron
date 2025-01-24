package org.folio.patron.rest.models;

public enum RequestLevel {
  ITEM("Item"),
  TITLE("Title");

  private final String value;

  RequestLevel(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
