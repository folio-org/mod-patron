package org.folio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import org.folio.repository.MediatedRequestsRepository;
import org.junit.jupiter.api.Test;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class MediatedRequestsServiceTest {

  private static final String REQUESTER_ID = "1ec54964-70f0-44cc-bd19-2a892ea0d336";

  @Test
  void buildActiveOrFailedBatchesQueryShouldFilterByRequesterStatusesAndRequestDate() {
    var cutoff = LocalDateTime.of(2026, 9, 16, 10, 15, 30);

    var query = MediatedRequestsService.buildActiveOrFailedBatchesQuery(REQUESTER_ID, cutoff);

    assertEquals(
      "requesterId==\"1ec54964-70f0-44cc-bd19-2a892ea0d336\""
        + " and (mediatedRequestStatus==\"Pending\""
        + " or mediatedRequestStatus==\"In progress\""
        + " or mediatedRequestStatus==\"Failed\")"
        + " and requestDate>=\"2026-09-16T10:15:30\""
        + " sortBy requestDate/sort.descending",
      query);
  }

  @Test
  void buildActiveOrFailedBatchesQueryShouldDropFractionalSeconds() {
    var cutoff = LocalDateTime.of(2026, 9, 16, 10, 15, 0, 123_000_000);

    var query = MediatedRequestsService.buildActiveOrFailedBatchesQuery(REQUESTER_ID, cutoff);

    assertTrue(query.contains("requestDate>=\"2026-09-16T10:15:00\" sortBy"));
  }

  @Test
  void findActiveOrFailedBatchIdsShouldGetAtMostMaxBatchesWithDaysBackCutoff() {
    var repository = new CapturingRepository(new JsonObject().put("mediatedBatchRequests", new JsonArray()
      .add(new JsonObject().put("batchId", "batch-1"))
      .add(new JsonObject().put("batchId", "batch-1"))
      .add(new JsonObject())
      .add(new JsonObject().put("batchId", "batch-2"))));
    var service = new MediatedRequestsService(repository);

    var batchIds = service.findActiveOrFailedBatchIds(REQUESTER_ID, 7, Map.of()).join();

    assertEquals(100, MediatedRequestsService.MAX_ACTIVE_OR_FAILED_BATCHES);
    assertEquals(MediatedRequestsService.MAX_ACTIVE_OR_FAILED_BATCHES, repository.limit);
    assertTrue(repository.query.startsWith("requesterId==\"" + REQUESTER_ID + "\""));
    assertTrue(repository.query.endsWith(" sortBy requestDate/sort.descending"));
    assertEquals(List.of("batch-1", "batch-2"), batchIds);

    var matcher = Pattern.compile("requestDate>=\"([^\"]+)\"").matcher(repository.query);
    assertTrue(matcher.find());
    var expectedCutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(7);
    var actualCutoff = LocalDateTime.parse(matcher.group(1));
    assertTrue(Math.abs(ChronoUnit.SECONDS.between(actualCutoff, expectedCutoff)) < 10);
  }

  @Test
  void findActiveOrFailedBatchIdsShouldGetEmptyListWhenCollectionHasNoBatches() {
    var repository = new CapturingRepository(new JsonObject());
    var service = new MediatedRequestsService(repository);

    var batchIds = service.findActiveOrFailedBatchIds(REQUESTER_ID, 0, Map.of()).join();

    assertTrue(batchIds.isEmpty());
    assertEquals(MediatedRequestsService.MAX_ACTIVE_OR_FAILED_BATCHES, repository.limit);
  }

  /** Repository stub that records the arguments of the collection call instead of calling mod-circulation-bff. */
  private static final class CapturingRepository extends MediatedRequestsRepository {
    private final JsonObject response;
    private String query;
    private int limit = -1;

    private CapturingRepository(JsonObject response) {
      super(null);
      this.response = response;
    }

    @Override
    public CompletableFuture<JsonObject> getBatchRequestsByQuery(String query, int limit,
      Map<String, String> okapiHeaders) {
      this.query = query;
      this.limit = limit;
      return CompletableFuture.completedFuture(response);
    }
  }
}
