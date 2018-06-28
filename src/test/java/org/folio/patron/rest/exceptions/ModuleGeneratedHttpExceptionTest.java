package org.folio.patron.rest.exceptions;

import static org.junit.Assert.*;

import org.folio.patron.utils.HttpExceptionMatcher;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ModuleGeneratedHttpExceptionTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public final void testModuleGeneratedHttpExceptionInt() throws HttpException {
    exception.expect(ModuleGeneratedHttpException.class);
    exception.expect(HttpExceptionMatcher.hasCode(5));

    throw new ModuleGeneratedHttpException(5);
  }

  @Test
  public final void testModuleGeneratedHttpExceptionIntString() throws HttpException {
    exception.expect(ModuleGeneratedHttpException.class);
    exception.expectMessage("this is a test");
    exception.expect(HttpExceptionMatcher.hasCode(5));

    throw new ModuleGeneratedHttpException(5, "this is a test");
  }

  @Test
  public final void testModuleGeneratedHttpExceptionIntThrowable() throws HttpException {
    exception.expect(ModuleGeneratedHttpException.class);
    exception.expectCause(Matchers.instanceOf(RuntimeException.class));
    exception.expect(HttpExceptionMatcher.hasCode(5));

    throw new ModuleGeneratedHttpException(5, new RuntimeException());
  }

  @Test
  public final void testModuleGeneratedHttpExceptionIntStringThrowable() throws HttpException {
    exception.expect(ModuleGeneratedHttpException.class);
    exception.expectCause(Matchers.instanceOf(RuntimeException.class));
    exception.expectMessage("this is a test");
    exception.expect(HttpExceptionMatcher.hasCode(5));

    throw new ModuleGeneratedHttpException(5, "this is a test", new RuntimeException());
  }

  @Test
  public final void testModuleGeneratedHttpExceptionIntStringThrowableBooleanBoolean() throws HttpException {
    exception.expect(ModuleGeneratedHttpException.class);
    exception.expectCause(Matchers.instanceOf(RuntimeException.class));
    exception.expectMessage("this is a test");
    exception.expect(HttpExceptionMatcher.hasCode(5));

    throw new ModuleGeneratedHttpException(5, "this is a test", new RuntimeException(), true, true);
  }

  @Test
  public final void testGetCode() {
    assertEquals(5, new ModuleGeneratedHttpException(5).getCode());
  }
}
