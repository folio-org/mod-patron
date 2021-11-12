package org.folio.rest.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.Hold;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import static org.folio.patron.utils.Utils.readMockFile;

class HoldHelpersTest {

  @Test
  void testAddCancellationFieldsToRequest() throws ParseException {

    String mockDataFolder = "PatronServicesResourceImpl";

    final String request = readMockFile(mockDataFolder + "/hold_cancel.json");
    JsonObject jsonRequest = new JsonObject(request);

    final String userIdCanceledHold = "f39fd3ca-e3fb-4cd9-8cf9-48e7e2c494e5";
    final String cancellationReasonId = "dd238b5b-01fc-4205-83b8-888888888888";
    final String requestId = "d0f032db-9579-443e-a2ac-6add937f4aa2";
    final String holdCancellationReason = "I really don't want it anymore";
    final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    Date canceledDate = DateTime.now().toDate();
    String canceledDateString = formatter.format(canceledDate);

    Hold holdEntity = new Hold();
    holdEntity.withStatus(Hold.Status.CLOSED_CANCELLED);
    holdEntity.withCanceledDate(canceledDate);
    holdEntity.withCanceledByUserId(userIdCanceledHold);
    holdEntity.withCancellationReasonId(cancellationReasonId);
    holdEntity.withCancellationAdditionalInformation(holdCancellationReason);
    holdEntity.withRequestId(requestId);

    final JsonObject newJsonRequest = HoldHelpers.addCancellationFieldsToRequest(jsonRequest, holdEntity);

    assertEquals(requestId, newJsonRequest.getString("id"));
    assertEquals(Hold.Status.CLOSED_CANCELLED.value(), newJsonRequest.getString("status"));
    assertEquals(userIdCanceledHold, newJsonRequest.getString("cancelledByUserId"));
    assertEquals(cancellationReasonId, newJsonRequest.getString("cancellationReasonId"));
    assertEquals(holdCancellationReason, newJsonRequest.getString("cancellationAdditionalInformation"));
    assertEquals(canceledDateString, newJsonRequest.getString("cancelledDate"));
  }
}
