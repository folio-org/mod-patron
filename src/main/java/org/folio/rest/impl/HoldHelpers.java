package org.folio.rest.impl;

import static java.util.Collections.singletonList;
import static org.folio.rest.impl.Constants.JSON_FIELD_CANCELLATION_ADDITIONAL_INFO;
import static org.folio.rest.impl.Constants.JSON_FIELD_CANCELLATION_DATE;
import static org.folio.rest.impl.Constants.JSON_FIELD_CANCELLATION_REASON_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_CANCELLATION_USER_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_CONTRIBUTORS;
import static org.folio.rest.impl.Constants.JSON_FIELD_ECS_REQUEST_PHASE;
import static org.folio.rest.impl.Constants.JSON_FIELD_FULFILLMENT_PREFERENCE;
import static org.folio.rest.impl.Constants.JSON_FIELD_HOLDINGS_RECORD_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_INSTANCE_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_ITEM;
import static org.folio.rest.impl.Constants.JSON_FIELD_ITEM_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_PATRON_COMMENTS;
import static org.folio.rest.impl.Constants.JSON_FIELD_PICKUP_SERVICE_POINT_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_POSITION;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUESTER;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUESTER_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_DATE;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_EXPIRATION_DATE;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_LEVEL;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_TYPE;
import static org.folio.rest.impl.Constants.JSON_FIELD_TITLE;

import java.text.SimpleDateFormat;
import java.util.Arrays;

import org.folio.patron.rest.exceptions.ValidationException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Hold;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Parameter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import io.vertx.core.json.JsonObject;

class HoldHelpers {

  private static final String STATUS_FIELD = "status";

  private HoldHelpers() {}

  static JsonObject addCancellationFieldsToRequest(JsonObject request, Hold entity) {

    final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    request.put("cancellationAdditionalInformation", entity.getCancellationAdditionalInformation());
    request.put("cancellationReasonId", entity.getCancellationReasonId());
    request.put("cancelledByUserId", entity.getCanceledByUserId());
    request.put("cancelledDate", formatter.format(entity.getCanceledDate()));
    request.put(STATUS_FIELD, Hold.Status.CLOSED_CANCELLED.value());
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
    String status = holdJson.getString(STATUS_FIELD);
    if (Arrays.stream(Hold.Status.values())
      .noneMatch(enumStatus -> enumStatus.value().equals(status))) {

      Error error = new Error()
        .withMessage("Invalid value of status")
        .withCode("INVALID_STATUS_VALUE")
        .withParameters(
          singletonList(new Parameter()
            .withKey(STATUS_FIELD)
            .withValue(status)));
      throw new ValidationException(new Errors().withErrors(singletonList(error)));
    }

    Hold hold = new Hold()
      .withItem(item)
      .withExpirationDate(holdJson.getString(JSON_FIELD_REQUEST_EXPIRATION_DATE) == null
        ? null
        : new DateTime(holdJson.getString(JSON_FIELD_REQUEST_EXPIRATION_DATE), DateTimeZone.UTC).toDate())
      .withRequestId(holdJson.getString("id"))
      .withPickupLocationId(holdJson.getString(JSON_FIELD_PICKUP_SERVICE_POINT_ID))
      .withRequestDate(new DateTime(holdJson.getString(JSON_FIELD_REQUEST_DATE), DateTimeZone.UTC).toDate())
      .withQueuePosition(holdJson.getInteger(JSON_FIELD_POSITION))
      .withStatus(Hold.Status.fromValue(status))
      .withCancellationAdditionalInformation(holdJson.getString(
        JSON_FIELD_CANCELLATION_ADDITIONAL_INFO))
      .withCancellationReasonId(holdJson.getString(JSON_FIELD_CANCELLATION_REASON_ID))
      .withCanceledByUserId(holdJson.getString(JSON_FIELD_CANCELLATION_USER_ID))
      .withPatronComments(holdJson.getString(JSON_FIELD_PATRON_COMMENTS));

    String canceledationDate = holdJson.getString(JSON_FIELD_CANCELLATION_DATE);
    if (canceledationDate != null && !canceledationDate.isEmpty()) {
      hold.withCanceledDate(new DateTime(canceledationDate, DateTimeZone.UTC).toDate());
    }
    return hold;
  }

  static JsonObject createCancelRequest(JsonObject body, Hold entity) {
    JsonObject itemJson = body.getJsonObject(JSON_FIELD_ITEM);
    if (itemJson != null) {
      itemJson.remove(JSON_FIELD_TITLE);
      itemJson.remove(JSON_FIELD_INSTANCE_ID);
      itemJson.remove(JSON_FIELD_CONTRIBUTORS);
    }

    JsonObject cancelRequest = new JsonObject()
      .put("id", body.getString(JSON_FIELD_ID))
      .put("requestLevel", body.getString(JSON_FIELD_REQUEST_LEVEL))
      .put("ecsRequestPhase", body.getString(JSON_FIELD_ECS_REQUEST_PHASE))
      .put("requestType", body.getString(JSON_FIELD_REQUEST_TYPE))
      .put("requestDate", body.getString(JSON_FIELD_REQUEST_DATE))
      .put("requesterId", body.getString(JSON_FIELD_REQUESTER_ID))
      .put("requester", body.getJsonObject(JSON_FIELD_REQUESTER))
      .put("instanceId", body.getString(JSON_FIELD_INSTANCE_ID))
      .put("holdingsRecordId", body.getString(JSON_FIELD_HOLDINGS_RECORD_ID))
      .put("itemId", body.getString(JSON_FIELD_ITEM_ID))
      .put("item", itemJson)
      .put("fulfillmentPreference", body.getString(JSON_FIELD_FULFILLMENT_PREFERENCE))
      .put("requestType", body.getString(JSON_FIELD_REQUEST_TYPE))
      .put("pickupServicePointId", body.getString(JSON_FIELD_PICKUP_SERVICE_POINT_ID))
      .put("patronComments", body.getString(JSON_FIELD_PATRON_COMMENTS));

    return addCancellationFieldsToRequest(cancelRequest, entity);
  }
}
