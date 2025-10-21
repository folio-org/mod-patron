package org.folio.patron.rest.models;

import lombok.Data;

@Data
public class BatchRequestItemStats {

  private int total;

  private int pending;

  private int inProgress;

  private int completed;

  private int failed;
}
