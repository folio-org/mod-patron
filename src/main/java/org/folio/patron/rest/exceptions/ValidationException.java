package org.folio.patron.rest.exceptions;

import org.folio.rest.jaxrs.model.Errors;

public class ValidationException extends RuntimeException {
  private final Errors errors;

  public ValidationException(Errors errors) {
    this.errors = errors;
  }

  public Errors getErrors() {
    return errors;
  }
}