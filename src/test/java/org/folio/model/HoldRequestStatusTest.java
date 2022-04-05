package org.folio.model;

import static org.folio.rest.jaxrs.model.Hold.Status.fromValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.rest.jaxrs.model.Hold;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HoldRequestStatusTest {

  @ParameterizedTest
  @ValueSource(strings = {"Open - Not yet filled", "Open - Awaiting pickup",
    "Open - Awaiting delivery", "Open - In transit", "Closed - Filled",
    "Closed - Cancelled", "Closed - Unfilled", "Closed - Pickup expired"
  })
  void testValidHoldRequestStatus(String status) {
    Hold.Status requestStatus = fromValue(status);
    assertNotNull(requestStatus);
    assertEquals(status, requestStatus.value());
  }

  @Test
  void testInvalidRequestStatus() {
    assertThrows(IllegalArgumentException.class, () -> fromValue("Invalid status"));
  }
}
