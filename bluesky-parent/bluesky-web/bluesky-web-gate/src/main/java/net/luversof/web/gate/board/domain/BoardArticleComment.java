package net.luversof.web.gate.board.domain;

import java.time.Instant;
import java.util.UUID;

public record BoardArticleComment(
    UUID id,
    UUID boardArticleId,
    UUID userId,
    String username,
    String content,
    Instant createdDate,
    Instant lastModifiedDate) {

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  public static class Builder {
    private UUID id;
    private UUID boardArticleId;
    private UUID userId;
    private String username;
    private String content;
    private Instant createdDate;
    private Instant lastModifiedDate;

    public Builder() {}

    public Builder(BoardArticleComment boardArticleComment) {
      this.id = boardArticleComment.id();
      this.boardArticleId = boardArticleComment.boardArticleId();
      this.userId = boardArticleComment.userId();
      this.username = boardArticleComment.username();
      this.content = boardArticleComment.content();
      this.createdDate = boardArticleComment.createdDate();
      this.lastModifiedDate = boardArticleComment.lastModifiedDate();
    }

    public Builder id(UUID id) {
      this.id = id;
      return this;
    }

    public Builder boardArticleId(UUID boardArticleId) {
      this.boardArticleId = boardArticleId;
      return this;
    }

    public Builder userId(UUID userId) {
      this.userId = userId;
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder content(String content) {
      this.content = content;
      return this;
    }

    public Builder createdDate(Instant createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    public Builder lastModifiedDate(Instant lastModifiedDate) {
      this.lastModifiedDate = lastModifiedDate;
      return this;
    }

    public BoardArticleComment build() {
      return new BoardArticleComment(
          id, boardArticleId, userId, username, content, createdDate, lastModifiedDate);
    }
  }
}
