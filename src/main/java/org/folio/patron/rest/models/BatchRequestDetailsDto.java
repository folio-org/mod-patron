package org.folio.patron.rest.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Mediated Batch Request Detail
 */
@Data
@NoArgsConstructor
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


  public BatchRequestDetailsDto batchId(String batchId) {
    this.batchId = batchId;
    return this;
  }

  public BatchRequestDetailsDto itemId(String itemId) {
    this.itemId = itemId;
    return this;
  }

  public BatchRequestDetailsDto requesterId(String requesterId) {
    this.requesterId = requesterId;
    return this;
  }

  public BatchRequestDetailsDto pickupServicePointId(String pickupServicePointId) {
    this.pickupServicePointId = pickupServicePointId;
    return this;
  }

  public BatchRequestDetailsDto mediatedRequestStatus(String mediatedRequestStatus) {
    this.mediatedRequestStatus = mediatedRequestStatus;
    return this;
  }

  public BatchRequestDetailsDto requestStatus(String requestStatus) {
    this.requestStatus = requestStatus;
    return this;
  }

  public BatchRequestDetailsDto confirmedRequestId(String confirmedRequestId) {
    this.confirmedRequestId = confirmedRequestId;
    return this;
  }

  public BatchRequestDetailsDto errorCode(String errorCode) {
    this.errorCode = errorCode;
    return this;
  }

  public BatchRequestDetailsDto errorDetails(String errorDetails) {
    this.errorDetails = errorDetails;
    return this;
  }

  public BatchRequestDetailsDto patronComments(String patronComments) {
    this.patronComments = patronComments;
    return this;
  }

  public BatchRequestDetailsDto metadata(Metadata metadata) {
    this.metadata = metadata;
    return this;
  }
}

