package org.folio.patron.rest.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class HttpExceptionTest {
  @Test
  public final void testHttpExceptionInt() {
    final HttpException thrown = assertThrows(HttpException.class,
        () -> { throw new HttpException(5); });
    assertEquals(5, thrown.getCode());
  }

  @Test
  public final void testHttpExceptionIntString() {
    final HttpException thrown = assertThrows(HttpException.class,
        () -> { throw new HttpException(5, "this is a test"); });
    assertEquals(5, thrown.getCode());
    assertEquals("this is a test", thrown.getMessage());
  }

  @Test
  public final void testHttpExceptionIntThrowable() {
    final HttpException thrown = assertThrows(HttpException.class,
        () -> { throw new HttpException(5, new RuntimeException()); });
    assertEquals(5, thrown.getCode());
    assertEquals(RuntimeException.class, thrown.getCause().getClass());
  }

  @Test
  public final void testHttpExceptionIntStringThrowable() {
    final HttpException thrown = assertThrows(HttpException.class,
        () -> { throw new HttpException(5, "this is a test", new RuntimeException()); });
    assertEquals(5, thrown.getCode());
    assertEquals("this is a test", thrown.getMessage());
    assertEquals(RuntimeException.class, thrown.getCause().getClass());
  }

  @Test
  public final void testHttpExceptionIntStringThrowableBooleanBoolean() {
    final HttpException thrown = assertThrows(HttpException.class, () -> {
        throw new HttpException(5, "this is a test", new RuntimeException(), true, true);
      });
    assertEquals(5, thrown.getCode());
    assertEquals("this is a test", thrown.getMessage());
    assertEquals(RuntimeException.class, thrown.getCause().getClass());
  }

  @Test
  public final void testGetCode() {
    assertEquals(5, new HttpException(5).getCode());
  }
}
