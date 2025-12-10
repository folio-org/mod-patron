package org.folio.support;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class OkapiHeaders {
  String okapiUrl;
  String tenantId;
  String token;

  public OkapiHeaders(OkapiUrl okapiUrl, String tenantId, String token) {
    this(okapiUrl.toString(), tenantId, token);
  }
}
