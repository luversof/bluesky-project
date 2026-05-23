package net.luversof.api.stock.web.dto.request;

import java.util.UUID;

public class MonthlyDividendSnapshotRequest {

  private UUID userId;

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }
}
