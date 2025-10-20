package org.folio.patron.rest.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * DTO for creating Mediated Batch Request
 */
@Getter
@RequiredArgsConstructor
public class BatchRequestPostDto {

  @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
  private String batchId;

  @NotNull
  @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
  private final String requesterId;

  @NotNull
  private final String mediatedWorkflow;

  @Valid
  @NotNull
  private final List<@Valid BatchItemRequests> itemRequests;

  private String patronComments;

  public BatchRequestPostDto batchId(String batchId) {
    this.batchId = batchId;
    return this;
  }

  public BatchRequestPostDto patronComments(String patronComments) {
    this.patronComments = patronComments;
    return this;
  }

  @Getter
  @RequiredArgsConstructor
  public static class BatchItemRequests {

    @NotNull
    @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
    private final String itemId;

    @NotNull
    @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
    private final String pickupServicePointId;
  }
}

