package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.Hold;
import org.folio.rest.jaxrs.model.Item;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.SimpleDateFormat;

class HoldHelpers {
  private HoldHelpers() {}

  static JsonObject addCancellationFieldsToRequest(JsonObject request, Hold entity) {

    final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    request.put("cancellationAdditionalInformation", entity.getCancellationAdditionalInformation());
    request.put("cancellationReasonId", entity.getCancellationReasonId());
    request.put("cancelledByUserId", entity.getCanceledByUserId());
    request.put("cancelledDate", formatter.format(entity.getCanceledDate()));
    request.put("status", Hold.Status.CLOSED_CANCELLED.value());
    return request;
  }

  static Hold constructNewHoldWithCancellationFields(Hold newHold, Hold tempHoldEntity) {
    newHold.withCancellationAdditionalInformation(tempHoldEntity.getCancellationAdditionalInformation());
    newHold.withCancellationReasonId(tempHoldEntity.getCancellationReasonId());
    newHold.withCanceledByUserId(tempHoldEntity.getCanceledByUserId());
    newHold.withCanceledDate(tempHoldEntity.getCanceledDate());
    newHold.withStatus(Hold.Status.CLOSED_CANCELLED);
    return newHold;
  }

  static Hold getHold(JsonObject holdJson, Item item) {
    Hold hold = new Hold()
      .withItem(item)
      .withExpirationDate(holdJson.getString(Constants.JSON_FIELD_REQUEST_EXPIRATION_DATE) == null
        ? null
        : new DateTime(holdJson.getString(Constants.JSON_FIELD_REQUEST_EXPIRATION_DATE), DateTimeZone.UTC).toDate())
      .withRequestId(holdJson.getString("id"))
      .withPickupLocationId(holdJson.getString(Constants.JSON_FIELD_PICKUP_SERVICE_POINT_ID))
      .withRequestDate(new DateTime(holdJson.getString(Constants.JSON_FIELD_REQUEST_DATE), DateTimeZone.UTC).toDate())
      .withQueuePosition(holdJson.getInteger(Constants.JSON_FIELD_POSITION))
      .withStatus(Hold.Status.fromValue(holdJson.getString("status")))
      .withCancellationAdditionalInformation(holdJson.getString(Constants.JSON_FIELD_CANCELLATION_ADDITIONAL_INFO))
      .withCancellationReasonId(holdJson.getString(Constants.JSON_FIELD_CANCELLATION_REASON_ID))
      .withCanceledByUserId(holdJson.getString(Constants.JSON_FIELD_CANCELLATION_USER_ID))
      .withPatronComments(holdJson.getString(Constants.JSON_FIELD_PATRON_COMMENTS));

    String canceledationDate = holdJson.getString(Constants.JSON_FIELD_CANCELLATION_DATE);
    if (canceledationDate != null && !canceledationDate.isEmpty()) {
      hold.withCanceledDate(new DateTime(canceledationDate, DateTimeZone.UTC).toDate());
    }
    return hold;
  }

  static JsonObject createCancelRequest(JsonObject body, Hold entity) {
    JsonObject itemJson = body.getJsonObject(Constants.JSON_FIELD_ITEM);
    itemJson.remove(Constants.JSON_FIELD_TITLE);
    itemJson.remove(Constants.JSON_FIELD_INSTANCE_ID);
    itemJson.remove(Constants.JSON_FIELD_CONTRIBUTORS);

    JsonObject cancelRequest = new JsonObject()
      .put("id", body.getString(Constants.JSON_FIELD_ID))
      .put("requestLevel", body.getString(Constants.JSON_FIELD_REQUEST_LEVEL))
      .put("requestType", body.getString(Constants.JSON_FIELD_REQUEST_TYPE))
      .put("requestDate", body.getString(Constants.JSON_FIELD_REQUEST_DATE))
      .put("requesterId", body.getString(Constants.JSON_FIELD_REQUESTER_ID))
      .put("requester", body.getJsonObject(Constants.JSON_FIELD_REQUESTER))
      .put("instanceId", body.getString(Constants.JSON_FIELD_INSTANCE_ID))
      .put("holdingsRecordId", body.getString(Constants.JSON_FIELD_HOLDINGS_RECORD_ID))
      .put("itemId", body.getString(Constants.JSON_FIELD_ITEM_ID))
      .put("item", itemJson)
      .put("fulfilmentPreference", body.getString(Constants.JSON_FIELD_FULFILMENT_PREFERENCE))
      .put("requestType", body.getString(Constants.JSON_FIELD_REQUEST_TYPE));

    return addCancellationFieldsToRequest(cancelRequest, entity);
  }
}
