package org.folio.patron.rest.models;

/**
 * Metadata about creation and changes to records, provided by the server (client should not provide)
 */
public class Metadata {

  private String createdDate;

  private String createdByUserId;

  private String createdByUsername;

  private String updatedDate;

  private String updatedByUserId;

  private String updatedByUsername;

  public Metadata() {}

  public Metadata(String createdDate, String createdByUserId, String createdByUsername, String updatedDate, String updatedByUserId, String updatedByUsername) {
    this.createdDate = createdDate;
    this.createdByUserId = createdByUserId;
    this.createdByUsername = createdByUsername;
    this.updatedDate = updatedDate;
    this.updatedByUserId = updatedByUserId;
    this.updatedByUsername = updatedByUsername;
  }

  public String getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(String createdDate) {
    this.createdDate = createdDate;
  }

  public String getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(String createdByUserId) {
    this.createdByUserId = createdByUserId;
  }

  public String getCreatedByUsername() {
    return createdByUsername;
  }

  public void setCreatedByUsername(String createdByUsername) {
    this.createdByUsername = createdByUsername;
  }

  public String getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(String updatedDate) {
    this.updatedDate = updatedDate;
  }

  public String getUpdatedByUserId() {
    return updatedByUserId;
  }

  public void setUpdatedByUserId(String updatedByUserId) {
    this.updatedByUserId = updatedByUserId;
  }

  public String getUpdatedByUsername() {
    return updatedByUsername;
  }

  public void setUpdatedByUsername(String updatedByUsername) {
    this.updatedByUsername = updatedByUsername;
  }

  @Override
  public String toString() {
    return "Metadata{" +
      "createdDate='" + createdDate + '\'' +
      ", createdByUserId='" + createdByUserId + '\'' +
      ", createdByUsername='" + createdByUsername + '\'' +
      ", updatedDate='" + updatedDate + '\'' +
      ", updatedByUserId='" + updatedByUserId + '\'' +
      ", updatedByUsername='" + updatedByUsername + '\'' +
      '}';
  }
}

