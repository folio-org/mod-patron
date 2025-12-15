package org.folio.support;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.vertx.core.Future;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.TenantAttributes;

public class TenantInit {
  private TenantInit() {}

  public static Future<Void> init(TenantClient tenantClient, TenantAttributes ta) {
    return tenantClient.postTenant(ta)
        .compose(res -> {
          if (res.statusCode() == 204) {
            return Future.succeededFuture();
          }
          assertThat("tenant POST status", res.statusCode(), is(201));
          assertThat("tenant POST error", res.bodyAsJsonObject().getString("error"), is(nullValue()));
          var jsonObject = res.bodyAsJsonObject();
          return tenantClient.getTenantByOperationId(jsonObject.getString("id"), 50000)
              .compose(res2 -> {
                assertThat("tenant GET status", res2.statusCode(), is(200));
                assertThat("tenant GET error", res2.bodyAsJsonObject().getString("error"), is(nullValue()));
                return tenantClient.deleteTenantByOperationId(jsonObject.getString("id"));
              })
              .map(res3 -> {
                assertThat("tenant DELETE status", res3.statusCode(), is(204));
                return null;
              });
        });
  }
}
