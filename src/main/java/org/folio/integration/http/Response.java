package org.folio.integration.http;

public class Response {
  public final int statusCode;
  public final String body;

  public Response(int statusCode, String body) {
    this.statusCode = statusCode;
    this.body = body;
  }

  public boolean isSuccess() {
    return statusCode >= 200 && statusCode < 300;
  }
}
