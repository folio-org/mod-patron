package org.folio.repository;

import static org.folio.rest.impl.Constants.JSON_FIELD_BATCH_REQUEST_ID;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  private static final Logger logger = LogManager.getLogger();
  private final VertxOkapiHttpClient client;

  public static final String CIRCULATION_BFF_BATCH_REQUESTS = "/circulation-bff/batch-requests";
  public static final String CIRCULATION_BFF_INSTANCE = "/circulation-bff/instance";

  private static final String JSON_FIELD_TOTAL_RECORDS = "totalRecords";
  private static final String JSON_COLLECTION_FIELD_BATCH_DETAILS = "mediatedBatchRequestDetails";

  /**
   * A batch's item details must never be truncated. mod-circulation-bff declares
   * limit with a default of 10, so omitting it silently caps the response at 10 rows.
   * MediatedRequestsService also recomputes itemsTotal/itemsRequested/itemsFailed/
   * itemsPending from this list for in-progress batches, so a partial page produces
   * wrong counts as well as missing details.
   */
  private static final Map<String, String> UNPAGED_DETAILS = Map.of(
    "limit", String.valueOf(Integer.MAX_VALUE),
    "offset", "0");

  public MediatedRequestsRepository(VertxOkapiHttpClient client) {
    this.client = client;
  }

  public CompletableFuture<JsonObject> createBatchRequest(BatchRequestPostDto postDto, Map<String, String> okapiHeaders) {
    logger.info("createBatchRequest:: Creating Batch Request");

    return client.post(CIRCULATION_BFF_BATCH_REQUESTS, JsonObject.mapFrom(postDto), okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .thenApply(result -> {
        if (result == null) {
          logger.warn("createBatchRequest:: null response received from POST {}", CIRCULATION_BFF_BATCH_REQUESTS);
          throw new ValidationException(
            buildErrors("POST Batch Multi-Item request returned null response", List.of()));
        }

        logger.info("createBatchRequest:: Successfully created batch request");
        return result;
      });
  }

  public CompletableFuture<JsonObject> getBatchRequestById(String batchId, Map<String, String> okapiHeaders) {
    logger.info("getBatchRequestById:: Retrieving Batch Request for batchId: {}", batchId);

    var endpoint = CIRCULATION_BFF_BATCH_REQUESTS + "/" + batchId;
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

  public CompletableFuture<JsonObject> getBatchRequestDetails(String instanceId, String batchId, Map<String, String> okapiHeaders) {
    logger.info("getBatchRequestDetails:: Retrieving Batch Request Details for batchId: {}", batchId);

    var endpoint = "%s/%s/batch-requests/%s/details".formatted(CIRCULATION_BFF_INSTANCE, instanceId, batchId);
    return client.get(endpoint, UNPAGED_DETAILS, okapiHeaders)
      .thenApply(ResponseInterpreter::verifyAndExtractBody)
      .thenApply(result -> {
        if (result == null) {
          logger.warn("getBatchRequestDetails:: null response received from GET {}", endpoint);
          throw new ValidationException(
            buildErrors("Getting Batch Multi-Item request details by batch ID returned null response",
              List.of(new Parameter().withKey(JSON_FIELD_BATCH_REQUEST_ID).withValue(batchId))));
        }

        warnIfTruncated(result, batchId, endpoint);

        logger.info("getBatchRequestDetails:: Successfully retrieved batch request details for batchId: {}", batchId);
        return result;
      });
  }

  /**
   * Guards against a silently truncated details page - the failure mode that caused
   * batches with more than 10 items to report incomplete item details.
   */
  private void warnIfTruncated(JsonObject result, String batchId, String endpoint) {
    var returned = Optional.ofNullable(result.getJsonArray(JSON_COLLECTION_FIELD_BATCH_DETAILS))
      .map(JsonArray::size)
      .orElse(0);
    var total = result.getInteger(JSON_FIELD_TOTAL_RECORDS, returned);

    if (returned < total) {
      logger.warn("warnIfTruncated:: truncated response from GET {} for batchId {}: got {} of {} details",
        endpoint, batchId, returned, total);
    }
  }

  private Errors buildErrors(String message, List<Parameter> params) {
    return new Errors()
      .withTotalRecords(1)
      .withErrors(
        List.of(new Error().withMessage(message).withParameters(params))
      );
  }
}
