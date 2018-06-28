package org.folio.patron.utils;

import org.folio.patron.rest.exceptions.HttpException;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class HttpExceptionMatcher extends TypeSafeMatcher<HttpException> {
  private int foundCode;
  private final int expectedCode;

  public static HttpExceptionMatcher hasCode(final int code) {
    return new HttpExceptionMatcher(code);
  }

  public HttpExceptionMatcher(final int expectedCode) {
    this.expectedCode = expectedCode;
  }

  @Override
  public void describeTo(final Description description) {
     description
       .appendValue(foundCode)
       .appendText(" was found instead of ")
       .appendValue(expectedCode);
  }

  @Override
  protected boolean matchesSafely(final HttpException exception) {
    foundCode = exception.getCode();

    return foundCode == expectedCode;
  }
}
