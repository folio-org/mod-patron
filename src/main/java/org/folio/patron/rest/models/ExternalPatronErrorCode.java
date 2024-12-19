package org.folio.patron.rest.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ExternalPatronErrorCode {
  MULTIPLE_USER_WITH_EMAIL("Multiple users found with the same email"),
  USER_ACCOUNT_INACTIVE("User account is not active"),
  USER_ALREADY_EXIST("User already exists in the remote patron group"),
  USER_NOT_FOUND("User does not exist"),
  STAGING_USER_NOT_FOUND("Staging user does not exist"),
  EMAIL_ALREADY_EXIST("User already exist with email provided in payload"),
  INVALID_PATRON_GROUP("User does not belong to the required patron group"),
  PATRON_GROUP_NOT_APPLICABLE("Required Patron group not applicable for user");

  private String value;

  ExternalPatronErrorCode(String value) {
    this.value = value;
  }
  @JsonValue
  public String value() {
    return value;
  }
}
