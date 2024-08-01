package org.folio.patron.rest.models;

import com.fasterxml.jackson.annotation.JsonValue;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;

import java.util.Collections;

public enum ExternalPatronErrorCode {
  MULTIPLE_USER_WITH_EMAIL("Multiple users found with the same email"),
  USER_ACCOUNT_INACTIVE("User account is not active"),
  USER_ALREADY_EXIST("User already exists"),
  USER_NOT_FOUND("User does not exist"),
  EMAIL_ALREADY_EXIST("User already exist with email provided in payload"),
  INVALID_PATRON_GROUP("User does not belong to the required patron group"),
  PATRON_GROUP_NOT_APPLICABLE("Required Patron group not applicable for user");

  String value;

  ExternalPatronErrorCode(String value) {
    this.value = value;
  }
  @JsonValue
  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return this.value;
  }

  public static Errors getErrors(ExternalPatronErrorCode mInstance) {
    return new Errors()
      .withErrors(Collections.singletonList(new Error().withMessage(mInstance.value()).withCode(mInstance.name())));
  }
}
