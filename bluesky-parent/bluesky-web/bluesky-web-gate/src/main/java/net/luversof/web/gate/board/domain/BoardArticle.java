package net.luversof.web.gate.board.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * 게시글 정보를 나타내는 domain 클래스 (bluesky-api-board와 매핑)
 */
public record BoardArticle(UUID id, UUID userId, String username, UUID boardId, String title, String content,
		Instant createdDate, Instant lastModifiedDate, Long commentCount) {

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public static class Builder {
		private UUID id;
		private UUID userId;
		private String username;
		private UUID boardId;
		private String title;
		private String content;
		private Instant createdDate;
		private Instant lastModifiedDate;
		private Long commentCount;

		public Builder() {
		}

		public Builder(BoardArticle boardArticle) {
			this.id = boardArticle.id();
			this.userId = boardArticle.userId();
			this.username = boardArticle.username();
			this.boardId = boardArticle.boardId();
			this.title = boardArticle.title();
			this.content = boardArticle.content();
			this.createdDate = boardArticle.createdDate();
			this.lastModifiedDate = boardArticle.lastModifiedDate();
			this.commentCount = boardArticle.commentCount();
		}

		public Builder id(UUID id) {
			this.id = id;
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

		public Builder boardId(UUID boardId) {
			this.boardId = boardId;
			return this;
		}

		public Builder title(String title) {
			this.title = title;
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

		public Builder commentCount(Long commentCount) {
			this.commentCount = commentCount;
			return this;
		}

		public BoardArticle build() {
			return new BoardArticle(id, userId, username, boardId, title, content, createdDate, lastModifiedDate,
					commentCount);
		}
	}
}