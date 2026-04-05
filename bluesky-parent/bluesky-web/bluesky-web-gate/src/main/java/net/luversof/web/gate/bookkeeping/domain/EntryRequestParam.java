package net.luversof.web.gate.bookkeeping.domain;

import java.time.ZonedDateTime;

public record EntryRequestParam(
    String bookkeepingId, String userId, ZonedDateTime startDate, ZonedDateTime endDate) {

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  public static class Builder {
    private String bookkeepingId;
    private String userId;
    private ZonedDateTime startDate;
    private ZonedDateTime endDate;

    public Builder() {}

    public Builder(EntryRequestParam entryRequestParam) {
      this.bookkeepingId = entryRequestParam.bookkeepingId();
      this.userId = entryRequestParam.userId();
      this.startDate = entryRequestParam.startDate();
      this.endDate = entryRequestParam.endDate();
    }

    public Builder bookkeepingId(String bookkeepingId) {
      this.bookkeepingId = bookkeepingId;
      return this;
    }

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder startDate(ZonedDateTime startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder endDate(ZonedDateTime endDate) {
      this.endDate = endDate;
      return this;
    }

    public EntryRequestParam build() {
      return new EntryRequestParam(bookkeepingId, userId, startDate, endDate);
    }
  }
}
