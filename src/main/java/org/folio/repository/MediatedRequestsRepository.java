package org.folio.repository;

import static org.folio.rest.impl.Constants.JSON_FIELD_BATCH_REQUEST_ID;

import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.integration.http.ResponseInterpreter;
import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.patron.rest.exceptions.ValidationException;
import org.folio.patron.rest.models.BatchRequestPostDto;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Parameter;


public class MediatedRequestsRepository {

  private static final String MEDIATED_BATCH_REQUEST = "/requests-mediated/batch-mediated-requests";

  private static final Logger logger = LogManager.getLogger();
  private final VertxOkapiHttpClient client;

  public MediatedRequestsRepository(VertxOkapiHttpClient client) {
    this.client = client;
  }

  public CompletableFuture<JsonObject> createBatchRequest(BatchRequestPostDto postDto, Map<String, String> okapiHeaders) {
    logger.info("createBatchRequest:: Creating Batch Request");

    return client.post(MEDIATED_BATCH_REQUEST, JsonObject.mapFrom(postDto), okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .thenApply(result -> {
        if (result == null) {
          logger.warn("createBatchRequest:: null response received from POST {}", MEDIATED_BATCH_REQUEST);
          throw new ValidationException(
            buildErrors("POST Batch Multi-Item request returned null response", List.of()));
        }

        logger.info("createBatchRequest:: Successfully created batch request");
        return result;
      });
  }

  public CompletableFuture<JsonObject> getBatchRequestById(String batchId, Map<String, String> okapiHeaders) {
    logger.info("getBatchRequestById:: Retrieving Batch Request for batchId: {}", batchId);

    var endpoint = MEDIATED_BATCH_REQUEST + "/" + batchId;
    return client.get(endpoint, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .thenApply(result -> {
        if (result == null) {
          logger.warn("getBatchRequestById:: null response received from GET {}", endpoint);
          throw new ValidationException(
            buildErrors("Getting Batch Multi-Item request by ID returned null response",
              List.of(new Parameter().withKey(JSON_FIELD_BATCH_REQUEST_ID).withValue(batchId))));
        }

        logger.info("getBatchRequestById:: Successfully retrieved batch request for batchId: {}", batchId);
        return result;
      });
  }

  public CompletableFuture<JsonObject> getBatchRequestDetails(String batchId, Map<String, String> okapiHeaders) {
    logger.info("getBatchRequestDetails:: Retrieving Batch Request Details for batchId: {}", batchId);

    var endpoint = MEDIATED_BATCH_REQUEST + "/" + batchId + "/details";
    return client.get(endpoint, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .thenApply(result -> {
        if (result == null) {
          logger.warn("getBatchRequestDetails:: null response received from GET {}", endpoint);
          throw new ValidationException(
            buildErrors("Getting Batch Multi-Item request details by batch ID returned null response",
              List.of(new Parameter().withKey(JSON_FIELD_BATCH_REQUEST_ID).withValue(batchId))));
        }

        logger.info("getBatchRequestDetails:: Successfully retrieved batch request details for batchId: {}", batchId);
        return result;
      });
  }

  private Errors buildErrors(String message, List<Parameter> params) {
    return new Errors()
      .withTotalRecords(1)
      .withErrors(
        List.of(new Error().withMessage(message).withParameters(params))
      );
  }
}
