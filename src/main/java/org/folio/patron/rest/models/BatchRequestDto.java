package org.folio.patron.rest.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.folio.rest.jaxrs.model.Metadata;

/**
 * DTO for Batch Request
 */
@Data
public class BatchRequestDto {

  @NotNull
  @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
  private String batchId;

  @NotNull
  @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
  private String requesterId;

  @NotNull
  private String mediatedRequestStatus;

  @NotNull
  private String requestDate;

  private BatchRequestItemStats itemRequestsStats;

  private String patronComments;

  @NotNull
  private String mediatedWorkflow;

  private Metadata metadata;
}

