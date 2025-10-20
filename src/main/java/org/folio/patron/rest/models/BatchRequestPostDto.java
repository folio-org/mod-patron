package org.folio.patron.rest.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * DTO for creating Mediated Batch Request
 */
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

  public BatchRequestPostDto(String requesterId, String mediatedWorkflow, List<@Valid BatchItemRequests> itemRequests) {
    this.requesterId = requesterId;
    this.mediatedWorkflow = mediatedWorkflow;
    this.itemRequests = itemRequests;
  }

  public String getBatchId() {
    return batchId;
  }

  public void setBatchId(String batchId) {
    this.batchId = batchId;
  }

  public String getRequesterId() {
    return requesterId;
  }

  public String getMediatedWorkflow() {
    return mediatedWorkflow;
  }

  public List<BatchItemRequests> getItemRequests() {
    return itemRequests;
  }

  public String getPatronComments() {
    return patronComments;
  }

  public void setPatronComments(String patronComments) {
    this.patronComments = patronComments;
  }

  public BatchRequestPostDto batchId(String batchId) {
    this.batchId = batchId;
    return this;
  }

  public BatchRequestPostDto patronComments(String patronComments) {
    this.patronComments = patronComments;
    return this;
  }

  public static class BatchItemRequests {

    @NotNull
    @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
    private final String itemId;

    @NotNull
    @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
    private final String pickupServicePointId;

    public BatchItemRequests(String itemId, String pickupServicePointId) {
      this.itemId = itemId;
      this.pickupServicePointId = pickupServicePointId;
    }

    public String getItemId() {
      return itemId;
    }

    public String getPickupServicePointId() {
      return pickupServicePointId;
    }
  }
}

