package org.folio.rest.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.ContributorName;
import org.folio.rest.jaxrs.model.Hold;
import org.folio.rest.jaxrs.model.Identifier;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Requester;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

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

  static Hold getHold(JsonObject holdJson) {
    final Item item = getItem(holdJson.getJsonObject(Constants.JSON_FIELD_ITEM));
    final Instance instance = getInstance(holdJson.getJsonObject(Constants.JSON_FIELD_INSTANCE));
    final Requester requester = getRequester(holdJson.getJsonObject(Constants.JSON_FIELD_REQUESTER));

    Hold hold = new Hold()
      .withItem(item)
      .withInstance(instance)
      .withRequester(requester)
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

  private static Item getItem(JsonObject body) {
    return new Item()
      .withBarcode(body.getString(Constants.JSON_FIELD_BARCODE));
  }

  private static Instance getInstance(JsonObject body) {

    List<Identifier> identifiers = new ArrayList<>();
    JsonArray identifiersArray = body.getJsonArray(Constants.JSON_FIELD_IDENTIFIERS);
    for (int index = 0; index < identifiersArray.size(); index++) {
      JsonObject jsonObject = identifiersArray.getJsonObject(index);
      identifiers.add(new Identifier()
      .withValue(jsonObject.getString("value"))
      .withIdentifierTypeId(jsonObject.getString("identifierTypeId")));
    }

    List<ContributorName> contributorNames = new ArrayList<>();
    JsonArray contributorNamesArray = body.getJsonArray(Constants.JSON_FIELD_CONTRIBUTOR_NAMES);
    for (int index = 0; index < contributorNamesArray.size(); index++) {
      JsonObject jsonObject = contributorNamesArray.getJsonObject(index);
      contributorNames.add(new ContributorName()
        .withName(jsonObject.getString("name")));
    }

    return new Instance()
      .withTitle(body.getString(Constants.JSON_FIELD_TITLE))
      .withIdentifiers(identifiers)
      .withContributorNames(contributorNames);
  }

  private static Requester getRequester(JsonObject body) {
    return new Requester()
      .withBarcode(body.getString(Constants.JSON_FIELD_BARCODE));
  }

}
