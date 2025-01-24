package org.folio.rest.impl;

import static org.folio.patron.rest.models.RequestLevel.ITEM;
import static org.folio.patron.rest.models.RequestLevel.TITLE;
import static org.folio.rest.impl.Constants.JSON_FIELD_FULFILLMENT_PREFERENCE;
import static org.folio.rest.impl.Constants.JSON_FIELD_INSTANCE_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_ITEM_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_PATRON_COMMENTS;
import static org.folio.rest.impl.Constants.JSON_FIELD_PICKUP_SERVICE_POINT_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUESTER_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_DATE;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_EXPIRATION_DATE;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_LEVEL;
import static org.folio.rest.impl.Constants.JSON_VALUE_HOLD_SHELF;

import org.folio.patron.rest.models.RequestLevel;
import org.folio.rest.jaxrs.model.Hold;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import io.vertx.core.json.JsonObject;

class RequestObjectFactory {

  public static JsonObject buildItemLevelRequest(Hold hold, String patronId, String itemId) {
    return buildRequest(hold, patronId, itemId, null, ITEM);
  }

  public static JsonObject buildTitleLevelRequest(String patronId, String instanceId, Hold hold) {
    return buildRequest(hold, patronId, null, instanceId, TITLE);
  }

  private static JsonObject buildRequest(Hold hold, String patronId, String itemId,
    String instanceId, RequestLevel requestLevel) {

    final JsonObject holdJSON = new JsonObject()
      .put(JSON_FIELD_REQUEST_LEVEL, requestLevel.getValue())
      .put(JSON_FIELD_ITEM_ID, itemId)
      .put(JSON_FIELD_INSTANCE_ID, instanceId)
      .put(JSON_FIELD_REQUESTER_ID, patronId)
      .put(JSON_FIELD_REQUEST_DATE, new DateTime(hold.getRequestDate(), DateTimeZone.UTC).toString())
      .put(JSON_FIELD_FULFILLMENT_PREFERENCE, JSON_VALUE_HOLD_SHELF)
      .put(JSON_FIELD_PICKUP_SERVICE_POINT_ID, hold.getPickupLocationId())
      .put(JSON_FIELD_PATRON_COMMENTS, hold.getPatronComments());

    if (hold.getExpirationDate() != null) {
      holdJSON.put(JSON_FIELD_REQUEST_EXPIRATION_DATE,
        new DateTime(hold.getExpirationDate(), DateTimeZone.UTC).toString());
    }

    return holdJSON;
  }

}
