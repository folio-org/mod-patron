package org.folio.patron.rest.exceptions;

public class UnexpectedFetchingException extends RuntimeException {
  public UnexpectedFetchingException(Throwable cause) {
    super(cause);
  }
}
