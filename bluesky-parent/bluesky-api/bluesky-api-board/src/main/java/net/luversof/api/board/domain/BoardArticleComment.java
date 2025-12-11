package net.luversof.api.board.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

@Table("BoardArticleComment")
public class BoardArticleComment {

	@Id
	@Column("id")
	@Null(groups = Create.class)
	@NotNull(groups = { Modify.class, Delete.class })
	private UUID id;

	@NotNull(groups = { Create.class })
	@Column("boardArticle_id")
	private UUID boardArticleId;

	@NotNull(groups = { Create.class, Modify.class, Delete.class })
	@Column("user_id")
	private UUID userId;

	@NotBlank(groups = { Create.class, Modify.class })
	@Column("content")
	private String content;

	@CreatedDate
	@Column("createdDate")
	private Instant createdDate;

	@LastModifiedDate
	@Column("lastModifiedDate")
	private Instant lastModifiedDate;

	public interface Create {
	}

	public interface Modify {
	}

	public interface Delete {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getBoardArticleId() {
		return boardArticleId;
	}

	public void setBoardArticleId(UUID boardArticleId) {
		this.boardArticleId = boardArticleId;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Instant getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Instant createdDate) {
		this.createdDate = createdDate;
	}

	public Instant getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(Instant lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BoardArticleComment other = (BoardArticleComment) obj;
		return Objects.equals(boardArticleId, other.boardArticleId) && Objects.equals(content, other.content)
				&& Objects.equals(createdDate, other.createdDate) && Objects.equals(id, other.id)
				&& Objects.equals(lastModifiedDate, other.lastModifiedDate) && Objects.equals(userId, other.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(boardArticleId, content, createdDate, id, lastModifiedDate, userId);
	}

	@Override
	public String toString() {
		return "BoardArticleComment [id=" + id + ", boardArticleId=" + boardArticleId + ", userId=" + userId
				+ ", content=" + content + ", createdDate=" + createdDate + ", lastModifiedDate=" + lastModifiedDate
				+ "]";
	}
}
