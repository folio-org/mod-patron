package org.folio.patron.rest.models;


public class BatchRequestItemStats {

  private int total;

  private int pending;

  private int inProgress;

  private int completed;

  private int failed;

  public BatchRequestItemStats() {}

  public BatchRequestItemStats(int total, int pending, int inProgress, int completed, int failed) {
    this.total = total;
    this.pending = pending;
    this.inProgress = inProgress;
    this.completed = completed;
    this.failed = failed;
  }

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public int getPending() {
    return pending;
  }

  public void setPending(int pending) {
    this.pending = pending;
  }

  public int getInProgress() {
    return inProgress;
  }

  public void setInProgress(int inProgress) {
    this.inProgress = inProgress;
  }

  public int getCompleted() {
    return completed;
  }

  public void setCompleted(int completed) {
    this.completed = completed;
  }

  public int getFailed() {
    return failed;
  }

  public void setFailed(int failed) {
    this.failed = failed;
  }

  @Override
  public String toString() {
    return "BatchRequestItemStats{" +
      "total=" + total +
      ", pending=" + pending +
      ", inProgress=" + inProgress +
      ", completed=" + completed +
      ", failed=" + failed +
      '}';
  }
}
