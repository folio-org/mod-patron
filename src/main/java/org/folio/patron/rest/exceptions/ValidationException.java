package org.folio.patron.rest.exceptions;


public class ValidationException extends RuntimeException {
  private final String exMessage;

  public ValidationException(String exMessage) {
    this.exMessage = exMessage;
  }

  public ValidationException(ValidationException exception){
    this.exMessage = exception.getExMessage();
  }

  public String getExMessage() {
    return exMessage;
  }
}
