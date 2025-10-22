package org.folio.patron.rest.models;

import lombok.Getter;

@Getter
public enum MediatedBatchRequestStatus {

  PENDING("Pending"),
  IN_PROGRESS("In progress"),
  COMPLETED("Completed"),
  FAILED("Failed");

  private final String value;

  MediatedBatchRequestStatus(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
