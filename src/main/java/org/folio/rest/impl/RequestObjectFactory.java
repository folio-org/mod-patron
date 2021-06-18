package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.Hold;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.folio.rest.impl.Constants.*;

class RequestObjectFactory {
  private final Map<String, String> okapiHeaders;

  RequestObjectFactory(Map<String, String> headers) {
    okapiHeaders = headers;
  }

  CompletableFuture<JsonObject> createRequestByItem(String patronId, String itemId, Hold entity) {
    return getRequestType(patronId, itemId)
      .thenApply(requestType -> {
        if (requestType != RequestType.NONE) {
          final JsonObject holdJSON = new JsonObject()
            .put(Constants.JSON_FIELD_ITEM_ID, itemId)
            .put("requesterId", patronId)
            .put("requestType", requestType.getValue())
            .put(Constants.JSON_FIELD_REQUEST_DATE, new DateTime(entity.getRequestDate(), DateTimeZone.UTC).toString())
            .put("fulfilmentPreference", Constants.JSON_VALUE_HOLD_SHELF)
            .put(Constants.JSON_FIELD_PICKUP_SERVICE_POINT_ID, entity.getPickupLocationId());

          if (entity.getExpirationDate() != null) {
            holdJSON.put(Constants.JSON_FIELD_REQUEST_EXPIRATION_DATE,
              new DateTime(entity.getExpirationDate(), DateTimeZone.UTC).toString());
          }
          return holdJSON;
        } else {
          return null;
        }
      });
  }

  private CompletableFuture<RequestType> getRequestType(String patronId, String itemId) {

    CompletableFuture<JsonObject> userFuture = LookupsUtils.getUser(patronId, okapiHeaders);
    CompletableFuture<JsonObject> itemFuture = LookupsUtils.getItem(itemId, okapiHeaders);

    RequestTypeParameters requestTypeParams = new RequestTypeParameters();

    return CompletableFuture.allOf(userFuture, itemFuture)
      .thenApply(x -> createRequestPolicyIdCriteria(itemFuture, userFuture, requestTypeParams))
      .thenCompose((RequestTypeParameters criteria) -> lookupRequestPolicyId(criteria))
      .thenCompose(policyIdResponse ->
          LookupsUtils.getRequestPolicy(policyIdResponse.getString("requestPolicyId"), okapiHeaders))
      .thenApply(RequestPolicy::from)
      .thenApply(requestPolicy -> getRequestType(requestPolicy, requestTypeParams.getItemStatus()))
      .exceptionally(t -> null);
  }

  private RequestType getRequestType(RequestPolicy policy,  ItemStatus itemStatus) {
    List<RequestType> allowableRequestTypes = policy.getRequestTypes();
    for (RequestType aRequestType : allowableRequestTypes) {
      if (RequestTypeItemStatusWhiteList.canCreateRequestForItem(itemStatus, aRequestType)){
        return aRequestType;
      }
    }
    return RequestType.NONE;
  }

  private CompletableFuture<JsonObject> lookupRequestPolicyId(RequestTypeParameters criteria) {
    String queryString = String.format(
      "item_type_id=%s&loan_type_id=%s&patron_type_id=%s&location_id=%s",
      criteria.getItemMaterialTypeId(), criteria.getItemLoanTypeId(),
      criteria.getPatronGroupId(), criteria.getItemLocationId()
    );

    return LookupsUtils.getRequestPolicyId(queryString, okapiHeaders);
  }

  private RequestTypeParameters createRequestPolicyIdCriteria(CompletableFuture<JsonObject> itemFuture,
                                                              CompletableFuture<JsonObject> userFuture,
                                                              RequestTypeParameters requestTypeParams) {
    JsonObject itemJson = itemFuture.join();
    requestTypeParams.setItemMaterialTypeId(getJsonObjectProperty(itemJson, "materialType", JSON_FIELD_ID));
    requestTypeParams.setItemLoanTypeId(getJsonObjectProperty(itemJson, "permanentLoanType", JSON_FIELD_ID));
    requestTypeParams.setItemLocationId(getJsonObjectProperty(itemJson, "effectiveLocation", JSON_FIELD_ID));
    requestTypeParams.setPatronGroupId(userFuture.join().getString(JSON_FIELD_PATRON_GROUP));
    requestTypeParams.setItemStatus(ItemStatus.from(getJsonObjectProperty(itemJson, "status", JSON_FIELD_NAME)));

    return requestTypeParams;
  }

  private String getJsonObjectProperty(JsonObject jsonObject, String objectName, String propertyName) {
    JsonObject destinedObject = jsonObject.getJsonObject(objectName);
    if (destinedObject != null) {
      return destinedObject.getString(propertyName);
    }
    return null;
  }
}
