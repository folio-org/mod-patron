package org.folio.patron.rest.exceptions;

public class ModuleGeneratedHttpException extends HttpException {
  private static final long serialVersionUID = -4647904231577953855L;

  public ModuleGeneratedHttpException(int code) {
    super(code);
  }

  public ModuleGeneratedHttpException(int code, String message) {
    super(code, message);
  }

  public ModuleGeneratedHttpException(int code, Throwable cause) {
    super(code, cause);
  }

  public ModuleGeneratedHttpException(int code, String message,
      Throwable cause) {
    super(code, message, cause);
  }

  public ModuleGeneratedHttpException(int code, String message, Throwable cause,
      boolean enableSuppression, boolean writableStackTrace) {
    super(code, message, cause, enableSuppression, writableStackTrace);
  }
}
