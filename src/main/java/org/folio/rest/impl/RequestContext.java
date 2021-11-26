package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.folio.rest.jaxrs.model.Hold;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@With
public class RequestContext {
  private final String patronId;
  private final String itemId;
  private final Hold hold;
  private JsonObject user;
  private JsonObject item;
  private RequestType requestType;
}
