package org.folio.patron.rest.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchRequestItemStats {

  private int total;

  private int pending;

  private int inProgress;

  private int completed;

  private int failed;
}
