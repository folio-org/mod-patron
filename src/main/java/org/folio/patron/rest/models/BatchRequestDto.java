package org.folio.patron.rest.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for Batch Request
 */
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

  public String getBatchId() {
    return batchId;
  }

  public void setBatchId(String batchId) {
    this.batchId = batchId;
  }

  public String getRequesterId() {
    return requesterId;
  }

  public void setRequesterId(String requesterId) {
    this.requesterId = requesterId;
  }

  public String getMediatedRequestStatus() {
    return mediatedRequestStatus;
  }

  public void setMediatedRequestStatus(String mediatedRequestStatus) {
    this.mediatedRequestStatus = mediatedRequestStatus;
  }

  public String getRequestDate() {
    return requestDate;
  }

  public void setRequestDate(String requestDate) {
    this.requestDate = requestDate;
  }

  public BatchRequestItemStats getItemRequestsStats() {
    return itemRequestsStats;
  }

  public void setItemRequestsStats(BatchRequestItemStats itemRequestsStats) {
    this.itemRequestsStats = itemRequestsStats;
  }

  public String getPatronComments() {
    return patronComments;
  }

  public void setPatronComments(String patronComments) {
    this.patronComments = patronComments;
  }

  public String getMediatedWorkflow() {
    return mediatedWorkflow;
  }

  public void setMediatedWorkflow(String mediatedWorkflow) {
    this.mediatedWorkflow = mediatedWorkflow;
  }

  public Metadata getMetadata() {
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  @Override
  public String toString() {
    return "BatchRequestDto{" +
      "batchId='" + batchId + '\'' +
      ", requesterId='" + requesterId + '\'' +
      ", mediatedRequestStatus='" + mediatedRequestStatus + '\'' +
      ", requestDate='" + requestDate + '\'' +
      ", itemRequestsStats=" + itemRequestsStats +
      ", patronComments='" + patronComments + '\'' +
      ", mediatedWorkflow='" + mediatedWorkflow + '\'' +
      ", metadata=" + metadata +
      '}';
  }
}

