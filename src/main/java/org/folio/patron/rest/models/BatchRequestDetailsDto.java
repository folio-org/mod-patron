package org.folio.patron.rest.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.folio.rest.jaxrs.model.Metadata;

/**
 * DTO for Mediated Batch Request Detail
 */
@Data
public class BatchRequestDetailsDto {

  @NotNull
  @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
  private String batchId;

  @NotNull
  @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
  private String itemId;

  @NotNull
  @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
  private String requesterId;

  @NotNull
  @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
  private String pickupServicePointId;

  @NotNull
  private String mediatedRequestStatus;

  private String requestStatus;

  @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
  private String confirmedRequestId;

  private String errorCode;

  private String errorDetails;

  private String patronComments;

  private Metadata metadata;
}

