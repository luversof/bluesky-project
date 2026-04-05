package net.luversof.web.gate.bookkeeping.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Bookkeeping(
    UUID id, UUID userId, String name, Instant createDate, Map<String, Object> jsonConfig) {

  // @Data
  // public static class BookeepingExtraData {
  // private int baseDate = 1;
  // }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  public static class Builder {
    private UUID id;
    private UUID userId;
    private String name;
    private Instant createDate;
    private Map<String, Object> jsonConfig;

    public Builder() {}

    public Builder(Bookkeeping bookkeeping) {
      this.id = bookkeeping.id();
      this.userId = bookkeeping.userId();
      this.name = bookkeeping.name();
      this.createDate = bookkeeping.createDate();
      this.jsonConfig = bookkeeping.jsonConfig();
    }

    public Builder id(UUID id) {
      this.id = id;
      return this;
    }

    public Builder userId(UUID userId) {
      this.userId = userId;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder createDate(Instant createDate) {
      this.createDate = createDate;
      return this;
    }

    public Builder jsonConfig(Map<String, Object> jsonConfig) {
      this.jsonConfig = jsonConfig;
      return this;
    }

    public Bookkeeping build() {
      return new Bookkeeping(id, userId, name, createDate, jsonConfig);
    }
  }
}
