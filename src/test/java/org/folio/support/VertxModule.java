package org.folio.support;

import java.util.LinkedList;
import java.util.List;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.Getter;
import lombok.NonNull;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;

public class VertxModule {
  @Getter
  private final Vertx vertx;

  private final WebClient webClient;
  public VertxModule(Vertx vertx) {
    this.vertx = vertx;
    webClient = WebClient.create(vertx);
  }

  public Future<String> deployModule(int port) {
    final var options = new DeploymentOptions()
      .setConfig(new JsonObject().put("http.port", port));

    return vertx.deployVerticle(RestVerticle.class.getName(), options);
  }

  public Future<Void> enableModule(@NonNull OkapiHeaders headers) {
    return enableModule(headers, false, false);
  }

  public Future<Void> enableModule(@NonNull OkapiHeaders headers,
    @NonNull Boolean loadReferenceData, @NonNull Boolean loadSampleData) {

    final var tenantClient = new TenantClient(headers.getOkapiUrl(),
      headers.getTenantId(), headers.getToken(), webClient);

    TenantAttributes ta = new TenantAttributes();
    ta.setModuleTo("mod-patron-999999.0.0");

    List<Parameter> parameters = new LinkedList<>();

    parameters.add(parameter("loadReference", loadReferenceData));
    parameters.add(parameter("loadSample", loadSampleData));

    ta.setParameters(parameters);

    return TenantInit.init(tenantClient, ta);
  }

  public Future<Void> purgeModule(@NonNull OkapiHeaders headers) {
    final var tenantClient = new TenantClient(headers.getOkapiUrl(),
        headers.getTenantId(), headers.getToken(), webClient);

    TenantAttributes ta = new TenantAttributes()
        .withPurge(true)
        .withModuleFrom("0.0.0");
    return TenantInit.init(tenantClient, ta);
  }

  public Future<Void> migrateModule(@NonNull OkapiHeaders headers, String versionFrom) {
    final var tenantClient = new TenantClient(headers.getOkapiUrl(),
        headers.getTenantId(), headers.getToken(), webClient);

    TenantAttributes ta = new TenantAttributes();
    ta.setModuleTo("999999.0.0");
    ta.setModuleFrom(versionFrom);

    List<Parameter> parameters = new LinkedList<>();

    parameters.add(parameter("loadReference", true));
    parameters.add(parameter("loadSample", true));

    ta.setParameters(parameters);

    return TenantInit.init(tenantClient, ta);
  }

  private Parameter parameter(String name, Boolean value) {
    return new Parameter().withKey(name).withValue(value.toString());
  }
}
