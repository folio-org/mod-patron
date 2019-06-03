package org.folio.patron.rest.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ModuleGeneratedHttpExceptionTest {
  @Test
  public final void testModuleGeneratedHttpExceptionInt() {
    final ModuleGeneratedHttpException thrown = assertThrows(ModuleGeneratedHttpException.class,
        () -> { throw new ModuleGeneratedHttpException(5); });
    assertEquals(5, thrown.getCode());
  }

  @Test
  public final void testModuleGeneratedHttpExceptionIntString() {
    final ModuleGeneratedHttpException thrown = assertThrows(ModuleGeneratedHttpException.class,
        () -> { throw new ModuleGeneratedHttpException(5, "this is a test"); });
    assertEquals(5, thrown.getCode());
    assertEquals("this is a test", thrown.getMessage());
  }

  @Test
  public final void testModuleGeneratedHttpExceptionIntThrowable() {
    final ModuleGeneratedHttpException thrown = assertThrows(ModuleGeneratedHttpException.class,
        () -> { throw new ModuleGeneratedHttpException(5, new RuntimeException()); });
    assertEquals(5, thrown.getCode());
    assertEquals(RuntimeException.class, thrown.getCause().getClass());
  }

  @Test
  public final void testModuleGeneratedHttpExceptionIntStringThrowable() {
    final ModuleGeneratedHttpException thrown = assertThrows(ModuleGeneratedHttpException.class,
        () -> { throw new ModuleGeneratedHttpException(5, "this is a test", new RuntimeException()); });
    assertEquals(5, thrown.getCode());
    assertEquals("this is a test", thrown.getMessage());
    assertEquals(RuntimeException.class, thrown.getCause().getClass());
  }

  @Test
  public final void testModuleGeneratedHttpExceptionIntStringThrowableBooleanBoolean() {
    final ModuleGeneratedHttpException thrown = assertThrows(ModuleGeneratedHttpException.class, () -> {
        throw new ModuleGeneratedHttpException(5, "this is a test", new RuntimeException(), true, true);
      });
    assertEquals(5, thrown.getCode());
    assertEquals("this is a test", thrown.getMessage());
    assertEquals(RuntimeException.class, thrown.getCause().getClass());
  }

  @Test
  public final void testGetCode() {
    assertEquals(5, new ModuleGeneratedHttpException(5).getCode());
  }
}
