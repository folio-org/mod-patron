package org.folio.service;

import static org.folio.rest.jaxrs.model.Batch.Status.COMPLETED;
import static org.folio.rest.jaxrs.model.Batch.Status.IN_PROGRESS;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.folio.integration.http.VertxOkapiHttpClient;
import org.folio.patron.rest.models.BatchRequestDetailsDto;
import org.folio.patron.rest.models.BatchRequestDto;
import org.folio.patron.rest.models.BatchRequestPostDto;
import org.folio.patron.rest.models.MediatedBatchRequestStatus;
import org.folio.repository.MediatedRequestsRepository;
import org.folio.rest.jaxrs.model.BatchRequest;
import org.folio.patron.rest.models.BatchRequestStatus;
import org.folio.rest.jaxrs.model.BatchRequestSubmitResult;
import org.folio.rest.jaxrs.model.ItemRequestsStats;
import org.folio.rest.jaxrs.model.ItemsFailedDetail;
import org.folio.rest.jaxrs.model.ItemsPendingDetail;
import org.folio.rest.jaxrs.model.ItemsRequestedDetail;
import org.folio.rest.jaxrs.model.Metadata;

@Slf4j
public class MediatedRequestsService {

  private static final String MEDIATED_BATCH_WORKFLOW = "Multi-Item request";
  private static final List<String> COMPLETED_MEDIATED_BATCH_REQUEST_STATUSES = List.of("Completed", "Failed");

  private static final List<MediatedBatchRequestStatus> ACTIVE_OR_FAILED_BATCH_REQUEST_STATUSES = List.of(
    MediatedBatchRequestStatus.PENDING,
    MediatedBatchRequestStatus.IN_PROGRESS,
    MediatedBatchRequestStatus.FAILED);

  /**
   * Upper bound of batches loaded for one account. Every batch costs two upstream calls (batch and details),
   * all fired concurrently, so this also bounds the burst of calls made by GET /patron/account/{id}.
   * The query is sorted by requestDate descending, so the oldest batches are the ones dropped.
   */
  static final int MAX_ACTIVE_OR_FAILED_BATCHES = 100;

  private final MediatedRequestsRepository repository;

  public MediatedRequestsService(VertxOkapiHttpClient httpClient) {
    this(new MediatedRequestsRepository(httpClient));
  }

  MediatedRequestsService(MediatedRequestsRepository repository) {
    this.repository = repository;
  }

  /**
   * CQL for mod-requests-mediated batch collection: the requester's batches that are not completed
   * successfully (pending, in progress or failed) and were requested at or after the cutoff.
   * The date term is a quoted local date-time without fraction; the upstream parses it with
   * LocalDateTime.parse.
   */
  static String buildActiveOrFailedBatchesQuery(String requesterId, LocalDateTime cutoff) {
    var statuses = ACTIVE_OR_FAILED_BATCH_REQUEST_STATUSES.stream()
      .map(status -> "mediatedRequestStatus==\"%s\"".formatted(status.getValue()))
      .collect(Collectors.joining(" or "));
    var cutoffTerm = cutoff.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    return "requesterId==\"%s\" and (%s) and requestDate>=\"%s\" sortBy requestDate/sort.descending"
      .formatted(requesterId, statuses, cutoffTerm);
  }

  public CompletableFuture<BatchRequestSubmitResult> createBatchRequest(BatchRequest batchRequest,
                                                                        String requesterId,
                                                                        Map<String, String> okapiHeaders) {
    var postDto = mapToBatchRequestPostDto(batchRequest, requesterId);
    return repository.createBatchRequest(postDto, okapiHeaders)
      .thenApply(createdJson -> createdJson.mapTo(BatchRequestDto.class))
      .thenApply(this::mapToBatchRequestSubmitResult);
  }

  public CompletableFuture<List<String>> findActiveOrFailedBatchIds(String requesterId, int daysBack,
                                                                    Map<String, String> okapiHeaders) {
    var cutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(daysBack);
    var query = buildActiveOrFailedBatchesQuery(requesterId, cutoff);

    return repository.getBatchRequestsByQuery(query, MAX_ACTIVE_OR_FAILED_BATCHES, okapiHeaders)
      .thenApply(json -> Optional.ofNullable(json.getJsonArray("mediatedBatchRequests"))
        .orElseGet(JsonArray::new)
        .stream()
        .map(JsonObject.class::cast)
        .map(batch -> batch.getString("batchId"))
        .filter(Objects::nonNull)
        .distinct()
        .toList());
  }

  public CompletableFuture<BatchRequestStatus> getBatchRequestStatus(String batchId, JsonObject instance,
                                                                    Map<String, String> okapiHeaders) {
    return getStatusFromBatchRequest(batchId, okapiHeaders)
      .thenCompose(batchStatus ->
        getBatchDetailsAndUpdateStatus(batchStatus, batchId, instance, okapiHeaders));
  }

  private CompletableFuture<BatchRequestStatus> getStatusFromBatchRequest(String batchId,
                                                                          Map<String, String> okapiHeaders) {
    return repository.getBatchRequestById(batchId, okapiHeaders)
      .thenApply(batchRequestJson -> batchRequestJson.mapTo(BatchRequestDto.class))
      .thenApply(batchRequestDto -> {
        BatchRequestStatus batchStatus = new BatchRequestStatus();
        batchStatus.setBatchRequestId(batchId);
        batchStatus.setSubmittedAt(Date.from(Instant.parse(batchRequestDto.getRequestDate())));

        if (COMPLETED_MEDIATED_BATCH_REQUEST_STATUSES.contains(batchRequestDto.getMediatedRequestStatus())) {
          batchStatus.setStatus(COMPLETED);
          Optional.ofNullable(batchRequestDto.getMetadata())
            .map(Metadata::getUpdatedDate)
            .ifPresent(batchStatus::setCompletedAt);
          Optional.ofNullable(batchRequestDto.getItemRequestsStats())
            .ifPresent(stats -> {
              batchStatus.setItemsTotal(stats.getTotal());
              batchStatus.setItemsFailed(stats.getFailed());
              batchStatus.setItemsPending(stats.getPending() + stats.getInProgress());
              batchStatus.setItemsRequested(stats.getCompleted());
            });
        } else {
          batchStatus.setStatus(IN_PROGRESS);
        }
        return batchStatus;
      });
  }

  private CompletableFuture<BatchRequestStatus> getBatchDetailsAndUpdateStatus(BatchRequestStatus batchStatus,
                                                                               String batchId, JsonObject instance,
                                                                               Map<String, String> okapiHeaders) {
    // null for batches found without an open request
    var instanceId = instance.getString("instanceId");
    var titleFromInstance = instance.getString("title");
    var detailsFuture = instanceId != null
      ? repository.getBatchRequestDetails(instanceId, batchId, okapiHeaders)
      : repository.getBatchRequestDetails(batchId, okapiHeaders);
    return detailsFuture
      .thenApply(detailsJson -> {
        var title = resolveTitle(titleFromInstance, detailsJson);
        var detailsDtoList = mapBatchRequestDetailsJsonToDto(detailsJson, batchId);

        var pendingItemsDetails = extractPendingItemsDetails(detailsDtoList, instanceId, title);
        batchStatus.setItemsPendingDetails(pendingItemsDetails);

        var failedItemsDetails = extractFailedItemsDetails(detailsDtoList, instanceId, title);
        batchStatus.setItemsFailedDetails(failedItemsDetails);

        var completedItemsDetails = extractRequestedItemsDetails(detailsDtoList, instanceId, title);
        batchStatus.setItemsRequestedDetails(completedItemsDetails);

        if (batchStatus.getStatus() == IN_PROGRESS) {
          // if batch request processing is still in progress, then data from /details is the most up-to-date
          batchStatus.setItemsTotal(detailsDtoList.size());
          batchStatus.setItemsRequested(completedItemsDetails.size());
          batchStatus.setItemsFailed(failedItemsDetails.size());
          batchStatus.setItemsPending(pendingItemsDetails.size());
        }

        return batchStatus;
      });
  }

  private String resolveTitle(String titleFromInstance, JsonObject detailsJson) {
    if (titleFromInstance != null) {
      return titleFromInstance;
    }

    return Optional.ofNullable(detailsJson.getJsonObject("instance"))
      .map(inst -> inst.getString("title"))
      .orElse(null);
  }

  private List<BatchRequestDetailsDto> mapBatchRequestDetailsJsonToDto(JsonObject detailsJson, String batchId) {
    var requestDetails = detailsJson.getJsonArray("mediatedBatchRequestDetails");
    if (requestDetails == null || requestDetails.isEmpty()) {
      log.warn("mapBatchRequestDetailsJsonToDto:: No request details found for batchId: {}", batchId);
      return List.of();
    }

    return requestDetails.stream()
      .map(JsonObject.class::cast)
      .map(jsobObj -> jsobObj.mapTo(BatchRequestDetailsDto.class))
      .toList();
  }

  private List<ItemsPendingDetail> extractPendingItemsDetails(List<BatchRequestDetailsDto> detailsDtoList,
                                                              String instanceId, String title) {
    return detailsDtoList.stream()
      .filter(detail -> isPendingOrInProgress(detail.getMediatedRequestStatus()))
      .map(detail -> new ItemsPendingDetail()
        .withItemId(detail.getItemId())
        .withPickUpLocationId(detail.getPickupServicePointId())
        .withInstanceId(instanceId)
        .withTitle(title))
      .toList();
  }

  private List<ItemsFailedDetail> extractFailedItemsDetails(List<BatchRequestDetailsDto> detailsDtoList,
                                                            String instanceId, String title) {
    return detailsDtoList.stream()
      .filter(detail -> MediatedBatchRequestStatus.FAILED.getValue().equals(detail.getMediatedRequestStatus()))
      .map(detail -> new ItemsFailedDetail()
        .withItemId(detail.getItemId())
        .withPickUpLocationId(detail.getPickupServicePointId())
        .withInstanceId(instanceId)
        .withTitle(title)
        .withErrorDetails(detail.getErrorDetails()))
      .toList();
  }

  private List<ItemsRequestedDetail> extractRequestedItemsDetails(List<BatchRequestDetailsDto> detailsDtoList,
                                                                  String instanceId, String title) {
    return detailsDtoList.stream()
      .filter(detail -> MediatedBatchRequestStatus.COMPLETED.getValue().equals(detail.getMediatedRequestStatus()))
      .map(detail -> new ItemsRequestedDetail()
        .withItemId(detail.getItemId())
        .withPickUpLocationId(detail.getPickupServicePointId())
        .withInstanceId(instanceId)
        .withTitle(title)
        .withConfirmedRequestId(detail.getConfirmedRequestId()))
      .toList();
  }

  private BatchRequestPostDto mapToBatchRequestPostDto(BatchRequest batchRequest, String requesterId) {
    var itemRequests = batchRequest.getRequests()
      .stream()
      .map(itemRequest -> new BatchRequestPostDto.BatchItemRequests(
        itemRequest.getItemId(),
        itemRequest.getPickUpLocationId()))
      .toList();

    BatchRequestPostDto dto = new BatchRequestPostDto(requesterId, MEDIATED_BATCH_WORKFLOW, itemRequests);
    if (batchRequest.getBatchRequestId() != null) {
      dto.batchId(batchRequest.getBatchRequestId());
    }
    if (batchRequest.getPatronComments() != null) {
      dto.patronComments(batchRequest.getPatronComments());
    }
    return dto;
  }

  private BatchRequestSubmitResult mapToBatchRequestSubmitResult(BatchRequestDto batchRequestDto) {
    var result = new BatchRequestSubmitResult()
      .withBatchId(batchRequestDto.getBatchId())
      .withRequesterId(batchRequestDto.getRequesterId())
      .withMediatedRequestStatus(
        BatchRequestSubmitResult.MediatedRequestStatus.fromValue(batchRequestDto.getMediatedRequestStatus()));

    Optional.ofNullable(batchRequestDto.getItemRequestsStats())
      .ifPresent(stats -> {
        var itemRequestsStats = new ItemRequestsStats()
          .withTotal(stats.getTotal())
          .withCompleted(stats.getCompleted())
          .withPending(stats.getPending())
          .withInProgress(stats.getInProgress())
          .withFailed(stats.getFailed());
        result.setItemRequestsStats(itemRequestsStats);
      });

    Optional.ofNullable(batchRequestDto.getMetadata())
      .ifPresent(metadata -> {
        var batchMetadata = new org.folio.rest.jaxrs.model.Metadata()
          .withCreatedByUserId(metadata.getCreatedByUserId())
          .withCreatedDate(metadata.getCreatedDate())
          .withCreatedByUsername(metadata.getCreatedByUsername())
          .withUpdatedByUserId(metadata.getUpdatedByUserId())
          .withUpdatedDate(metadata.getUpdatedDate())
          .withUpdatedByUsername(metadata.getUpdatedByUsername());
        result.setMetadata(batchMetadata);
      });

    return result;
  }

  private boolean isPendingOrInProgress(String status) {
    return MediatedBatchRequestStatus.PENDING.getValue().equals(status)
      || MediatedBatchRequestStatus.IN_PROGRESS.getValue().equals(status);
  }
}
