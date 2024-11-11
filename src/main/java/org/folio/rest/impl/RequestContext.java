package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.Hold;

public class RequestContext {
  private final boolean isEcsTlrFeatureEnabled;
  private final String patronId;
  private final String itemId;
  private String instanceId;
  private final Hold hold;
  private JsonObject user;
  private JsonObject item;
  private RequestTypeParameters requestTypeParams;
  private JsonObject requestPolicyId ;
  private RequestPolicy requestPolicy;
  private RequestType requestType;
  private JsonObject holdRequest;

  public RequestContext(boolean isEcsTlrFeatureEnabled, String patronId, String itemId, Hold hold) {
    this.isEcsTlrFeatureEnabled = isEcsTlrFeatureEnabled;
    this.patronId = patronId;
    this.itemId = itemId;
    this.hold = hold;
  }

  public boolean isEcsTlrFeatureEnabled() {
    return isEcsTlrFeatureEnabled;
  }

  public String getPatronId() {
    return patronId;
  }

  public String getItemId() {
    return itemId;
  }

  public Hold getHold() {
    return hold;
  }

  public JsonObject getUser() {
    return user;
  }

  public RequestContext setUser(JsonObject user) {
    this.user = user;
    return this;
  }

  public JsonObject getItem() {
    return item;
  }

  public RequestContext setItem(JsonObject item) {
    this.item = item;
    return this;
  }

  public RequestContext setInstanceId(JsonObject holdingsRecord) {
    if (holdingsRecord != null) {
      this.instanceId = holdingsRecord.getString("instanceId");
    }
    return this;
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public RequestContext setRequestType(RequestType requestType) {
    this.requestType = requestType;
    return this;
  }

  public RequestTypeParameters getRequestTypeParams() {
    return requestTypeParams;
  }

  public RequestContext setRequestTypeParams(RequestTypeParameters requestTypeParams) {
    this.requestTypeParams = requestTypeParams;
    return this;
  }

  public JsonObject getRequestPolicyId() {
    return requestPolicyId;
  }

  public RequestContext setRequestPolicyId(JsonObject requestPolicyId) {
    this.requestPolicyId = requestPolicyId;
    return this;
  }

  public RequestPolicy getRequestPolicy() {
    return requestPolicy;
  }

  public RequestContext setRequestPolicy(RequestPolicy requestPolicy) {
    this.requestPolicy = requestPolicy;
    return this;
  }

  public JsonObject getHoldRequest() {
    return holdRequest;
  }

  public RequestContext setHoldRequest(JsonObject holdRequest) {
    this.holdRequest = holdRequest;
    return this;
  }
}
