package org.folio.patron.rest.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MediatedBatchRequestStatus {

  PENDING("Pending"),
  IN_PROGRESS("In progress"),
  COMPLETED("Completed"),
  FAILED("Failed");

  private final String value;

  MediatedBatchRequestStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static MediatedBatchRequestStatus fromValue(String value) {
    for (MediatedBatchRequestStatus b : MediatedBatchRequestStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
