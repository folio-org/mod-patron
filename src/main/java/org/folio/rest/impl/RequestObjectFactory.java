package org.folio.rest.impl;

import static org.folio.rest.impl.Constants.JSON_FIELD_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_NAME;
import static org.folio.rest.impl.Constants.JSON_FIELD_PATRON_GROUP;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.rest.jaxrs.model.Hold;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

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
            .put(Constants.JSON_FIELD_PICKUP_SERVICE_POINT_ID, entity.getPickupLocationId())
            .put(Constants.JSON_FIELD_PATRON_COMMENTS, entity.getPatronComments());

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
    final var itemRepository = new ItemRepository();

    CompletableFuture<JsonObject> userFuture = LookupsUtils.getUser(patronId, okapiHeaders);
    CompletableFuture<JsonObject> itemFuture = itemRepository.getItem(itemId, okapiHeaders);

    RequestTypeParameters requestTypeParams = new RequestTypeParameters();

    return CompletableFuture.allOf(userFuture, itemFuture)
      .thenApply(x -> createRequestPolicyIdCriteria(itemFuture, userFuture, requestTypeParams))
      .thenCompose(this::lookupRequestPolicyId)
      .thenCompose(policyIdResponse ->
          getRequestPolicy(policyIdResponse.getString("requestPolicyId"), okapiHeaders))
      .thenApply(RequestPolicy::from)
      .thenApply(requestPolicy -> getRequestType(requestPolicy, requestTypeParams.getItemStatus()));
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
    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    final var queryParameters = Map.of(
      "item_type_id", criteria.getItemMaterialTypeId(),
      "loan_type_id", criteria.getItemLoanTypeId(),
      "patron_type_id", criteria.getPatronGroupId(),
      "location_id", criteria.getItemLocationId());

    return client.get("/circulation/rules/request-policy", queryParameters, okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
  }

  private CompletableFuture<JsonObject> getRequestPolicy(String requestPolicyId, Map<String, String> okapiHeaders) {
    final var client = new VertxOkapiHttpClient(Vertx.currentContext().owner());

    return client.get("/request-policy-storage/request-policies/" + requestPolicyId,
        Map.of(), okapiHeaders)
      .thenApply(LookupsUtils::verifyAndExtractBody);
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
