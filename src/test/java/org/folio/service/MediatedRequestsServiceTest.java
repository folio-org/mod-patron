package org.folio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MediatedRequestsServiceTest {

  @Test
  void buildActiveOrFailedBatchesQuery_shouldFilterByRequesterStatusesAndRequestDate() {
    var cutoff = LocalDateTime.of(2026, 9, 16, 10, 15, 30);

    var query = MediatedRequestsService.buildActiveOrFailedBatchesQuery(
      "1ec54964-70f0-44cc-bd19-2a892ea0d336", cutoff);

    assertEquals(
      "requesterId==\"1ec54964-70f0-44cc-bd19-2a892ea0d336\""
        + " and (mediatedRequestStatus==\"Pending\""
        + " or mediatedRequestStatus==\"In progress\""
        + " or mediatedRequestStatus==\"Failed\")"
        + " and requestDate>=\"2026-09-16T10:15:30\"",
      query);
  }

  @Test
  void buildActiveOrFailedBatchesQueryShouldDropFractionalSeconds() {
    var cutoff = LocalDateTime.of(2026, 9, 16, 10, 15, 0, 123_000_000);

    var query = MediatedRequestsService.buildActiveOrFailedBatchesQuery(
      "1ec54964-70f0-44cc-bd19-2a892ea0d336", cutoff);

    assertEquals(true, query.endsWith("requestDate>=\"2026-09-16T10:15:00\""));
  }
}
