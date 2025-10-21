package org.folio.repository;

import static org.folio.rest.impl.Constants.JSON_FIELD_INSTANCE_ID;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.integration.http.ResponseInterpreter;
import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.patron.rest.exceptions.ValidationException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Parameter;


public class InstanceRepository {

  private static final String INVENTORY_INSTANCE_PATH = "/inventory/instances";

  private static final Logger logger = LogManager.getLogger();
  private final VertxOkapiHttpClient client;

  public InstanceRepository(VertxOkapiHttpClient client) {
    this.client = client;
  }

  public CompletableFuture<JsonObject> getInstance(String instanceId, Map<String, String> okapiHeaders) {
    return client.get(INVENTORY_INSTANCE_PATH + "/" + instanceId, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .thenApply(result -> {
        if (result == null) {
          var errors = new Errors()
            .withErrors(List.of(new Error()
              .withMessage("null response returned to get instance by id from GET " + INVENTORY_INSTANCE_PATH)
              .withParameters(List.of(new Parameter().withKey(JSON_FIELD_INSTANCE_ID).withValue(instanceId))))
            );
          throw new ValidationException(errors);
        }

        logger.info("getInstance:: Successfully retrieved inventory instance for instanceId: {}", instanceId);
        return result;
      });
  }
}
