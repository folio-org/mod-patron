package org.folio.patron.rest.exceptions;

import static org.junit.Assert.*;

import org.folio.patron.utils.HttpExceptionMatcher;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HttpExceptionTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public final void testHttpExceptionInt() throws HttpException {
    exception.expect(HttpException.class);
    exception.expect(HttpExceptionMatcher.hasCode(5));

    throw new HttpException(5);
  }

  @Test
  public final void testHttpExceptionIntString() throws HttpException {
    exception.expect(HttpException.class);
    exception.expectMessage("this is a test");
    exception.expect(HttpExceptionMatcher.hasCode(5));

    throw new HttpException(5, "this is a test");
  }

  @Test
  public final void testHttpExceptionIntThrowable() throws HttpException {
    exception.expect(HttpException.class);
    exception.expectCause(Matchers.instanceOf(RuntimeException.class));
    exception.expect(HttpExceptionMatcher.hasCode(5));

    throw new HttpException(5, new RuntimeException());
  }

  @Test
  public final void testHttpExceptionIntStringThrowable() throws HttpException {
    exception.expect(HttpException.class);
    exception.expectCause(Matchers.instanceOf(RuntimeException.class));
    exception.expectMessage("this is a test");
    exception.expect(HttpExceptionMatcher.hasCode(5));

    throw new HttpException(5, "this is a test", new RuntimeException());
  }

  @Test
  public final void testHttpExceptionIntStringThrowableBooleanBoolean() throws HttpException {
    exception.expect(HttpException.class);
    exception.expectCause(Matchers.instanceOf(RuntimeException.class));
    exception.expectMessage("this is a test");
    exception.expect(HttpExceptionMatcher.hasCode(5));

    throw new HttpException(5, "this is a test", new RuntimeException(), true, true);
  }

  @Test
  public final void testGetCode() {
    assertEquals(5, new HttpException(5).getCode());
  }
}
