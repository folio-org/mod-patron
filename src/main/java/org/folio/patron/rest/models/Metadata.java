package org.folio.patron.rest.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata about creation and changes to records, provided by the server (client should not provide)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Metadata {

  private String createdDate;

  private String createdByUserId;

  private String createdByUsername;

  private String updatedDate;

  private String updatedByUserId;

  private String updatedByUsername;
}

