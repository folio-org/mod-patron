package org.folio.rest.impl;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.impl.Constants.JSON_FIELD_FULFILLMENT_PREFERENCE;
import static org.folio.rest.impl.Constants.JSON_FIELD_HOLDINGS_RECORD_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_INSTANCE_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_ITEM_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_NAME;
import static org.folio.rest.impl.Constants.JSON_FIELD_PATRON_COMMENTS;
import static org.folio.rest.impl.Constants.JSON_FIELD_PATRON_GROUP;
import static org.folio.rest.impl.Constants.JSON_FIELD_PICKUP_SERVICE_POINT_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUESTER_ID;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_DATE;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_EXPIRATION_DATE;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_LEVEL;
import static org.folio.rest.impl.Constants.JSON_FIELD_REQUEST_TYPE;
import static org.folio.rest.impl.Constants.JSON_VALUE_HOLD_SHELF;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.folio.integration.http.ResponseInterpreter;
import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.patron.rest.exceptions.ValidationException;
import org.folio.rest.jaxrs.model.Hold;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import io.vertx.core.json.JsonObject;

class RequestObjectFactory {
  private final Map<String, String> okapiHeaders;
  private final VertxOkapiHttpClient httpClient;
  private final ItemRepository itemRepository;
  private final UserRepository userRepository;
  private final HoldingsRecordRepository holdingsRecordRepository;

  RequestObjectFactory(VertxOkapiHttpClient httpClient, Map<String, String> okapiHeaders) {
    this.okapiHeaders = okapiHeaders;
    this.httpClient = httpClient;
    this.itemRepository = new ItemRepository(httpClient);
    this.userRepository = new UserRepository(httpClient);
    this.holdingsRecordRepository = new HoldingsRecordRepository(httpClient);
  }

  CompletableFuture<RequestContext> createRequestByItem(boolean isEcsTlrFeatureEnabled, String patronId, String itemId, Hold entity) {

    return completedFuture(new RequestContext(isEcsTlrFeatureEnabled, patronId, itemId, entity))
      .thenCompose(this::fetchItem)
      .thenCompose(this::fetchInstanceId)
      .thenCompose(this::fetchUser)
      .thenCompose(this::fetchRequestType)
      .thenApply(context -> {
        if (context.getRequestType() == RequestType.NONE) {
          return null;
        }
        JsonObject holdJSON = new JsonObject()
          .put(JSON_FIELD_REQUEST_LEVEL, "Item")
          .put(JSON_FIELD_INSTANCE_ID, context.getInstanceId())
          .put(JSON_FIELD_ITEM_ID, itemId)
          .put(JSON_FIELD_HOLDINGS_RECORD_ID,
            context.getItem().getString(JSON_FIELD_HOLDINGS_RECORD_ID))
          .put(JSON_FIELD_REQUESTER_ID, patronId)
          .put(JSON_FIELD_REQUEST_TYPE, context.getRequestType().getValue())
          .put(JSON_FIELD_REQUEST_DATE, new DateTime(entity.getRequestDate(), DateTimeZone.UTC).toString())
          .put(JSON_FIELD_FULFILLMENT_PREFERENCE, JSON_VALUE_HOLD_SHELF)
          .put(JSON_FIELD_PICKUP_SERVICE_POINT_ID, entity.getPickupLocationId())
          .put(JSON_FIELD_PATRON_COMMENTS, entity.getPatronComments());

        if (entity.getExpirationDate() != null) {
          holdJSON.put(JSON_FIELD_REQUEST_EXPIRATION_DATE,
            new DateTime(entity.getExpirationDate(), DateTimeZone.UTC).toString());
        }
        return context.setHoldRequest(holdJSON);
      });
  }

  private CompletableFuture<RequestContext> fetchItem(RequestContext requestContext) {
    return itemRepository.getItem(requestContext.getItemId(), okapiHeaders)
      .thenApply(requestContext::setItem);
  }

  private CompletableFuture<RequestContext> fetchInstanceId(RequestContext requestContext)
    throws ValidationException {

    return holdingsRecordRepository.getHoldingsRecord(requestContext.getItem(), okapiHeaders)
      .thenApply(requestContext::setInstanceId);
  }

  private CompletableFuture<RequestContext> fetchUser(RequestContext requestContext) {
    return userRepository.getUser(requestContext.getPatronId(), okapiHeaders)
      .thenApply(requestContext::setUser);
  }

  private CompletableFuture<RequestContext> fetchRequestType(RequestContext requestContext) {
    if (requestContext.isEcsTlrFeatureEnabled()) {
      requestContext.setRequestType(RequestType.PAGE);
    }
    return CompletableFuture.completedFuture(createRequestPolicyIdCriteria(requestContext))
      .thenCompose(this::lookupRequestPolicyId)
      .thenCompose(this::getRequestPolicy)
      .thenApply(context -> RequestPolicy.from(context.getRequestPolicyId()))
      .thenApply(requestContext::setRequestPolicy)
      .thenApply(this::getRequestType)
      .thenApply(requestContext::setRequestType);
  }

  private RequestContext createRequestPolicyIdCriteria(RequestContext requestContext) {
    JsonObject itemJson = requestContext.getItem();
    RequestTypeParameters requestTypeParams = new RequestTypeParameters();
    requestTypeParams.setItemMaterialTypeId(getJsonObjectProperty(itemJson, "materialType", JSON_FIELD_ID));
    requestTypeParams.setItemLoanTypeId(getJsonObjectProperty(itemJson, "permanentLoanType", JSON_FIELD_ID));
    requestTypeParams.setItemLocationId(getJsonObjectProperty(itemJson, "effectiveLocation", JSON_FIELD_ID));
    requestTypeParams.setPatronGroupId(requestContext.getUser().getString(JSON_FIELD_PATRON_GROUP));
    requestTypeParams.setItemStatus(ItemStatus.from(getJsonObjectProperty(itemJson, "status", JSON_FIELD_NAME)));

    return requestContext
      .setRequestTypeParams(requestTypeParams);
  }

  private String getJsonObjectProperty(JsonObject jsonObject, String objectName, String propertyName) {
    return Optional.of(jsonObject.getJsonObject(objectName))
      .map(entries -> entries.getString(propertyName))
      .orElse(null);
  }

  private CompletableFuture<RequestContext> lookupRequestPolicyId(RequestContext requestContext) {
    RequestTypeParameters criteria = requestContext.getRequestTypeParams();

    final var queryParameters = Map.of(
      "item_type_id", criteria.getItemMaterialTypeId(),
      "loan_type_id", criteria.getItemLoanTypeId(),
      "patron_type_id", criteria.getPatronGroupId(),
      "location_id", criteria.getItemLocationId());

    return httpClient.get("/circulation/rules/request-policy", queryParameters, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .thenApply(requestContext::setRequestPolicyId);
  }

  private CompletableFuture<RequestContext> getRequestPolicy(RequestContext requestContext) {
    return httpClient.get("/request-policy-storage/request-policies/" + requestContext.getRequestPolicyId().getString("requestPolicyId"),
        Map.of(), okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .thenApply(requestContext::setRequestPolicyId);
  }

  private RequestType getRequestType(RequestContext requestContext) {
    return requestContext.getRequestPolicy().getRequestTypes().stream()
      .filter(aRequestType -> RequestTypeItemStatusWhiteList
        .canCreateRequestForItem(requestContext.getRequestTypeParams().getItemStatus(), aRequestType))
      .findFirst()
      .orElse(RequestType.NONE);
  }
}
