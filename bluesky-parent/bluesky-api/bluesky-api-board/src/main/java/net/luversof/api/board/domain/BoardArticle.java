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

@Table("BoardArticle")
public class BoardArticle {

  @Id
  @Column("id")
  @Null(groups = Create.class)
  @NotNull(groups = {Get.class})
  private UUID id;

  @NotNull(groups = {Create.class, Modify.class, Delete.class})
  @Column("user_id")
  private UUID userId;

  @NotNull(groups = {Create.class})
  @Column("board_id")
  private UUID boardId;

  @NotBlank(groups = {Create.class, Modify.class})
  @Column("title")
  private String title;

  @NotBlank(groups = {Create.class, Modify.class})
  @Column("content")
  private String content;

  @CreatedDate
  @Column("createdDate")
  private Instant createdDate;

  @LastModifiedDate
  @Column("lastModifiedDate")
  private Instant lastModifiedDate;

  public interface Create {}

  public interface Get {}

  public interface Modify {}

  public interface Delete {}

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UUID getBoardId() {
    return boardId;
  }

  public void setBoardId(UUID boardId) {
    this.boardId = boardId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
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
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    BoardArticle other = (BoardArticle) obj;
    return Objects.equals(boardId, other.boardId)
        && Objects.equals(content, other.content)
        && Objects.equals(createdDate, other.createdDate)
        && Objects.equals(id, other.id)
        && Objects.equals(lastModifiedDate, other.lastModifiedDate)
        && Objects.equals(title, other.title)
        && Objects.equals(userId, other.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(boardId, content, createdDate, id, lastModifiedDate, title, userId);
  }

  @Override
  public String toString() {
    return "BoardArticle [id="
        + id
        + ", userId="
        + userId
        + ", boardId="
        + boardId
        + ", title="
        + title
        + ", content="
        + content
        + ", createdDate="
        + createdDate
        + ", lastModifiedDate="
        + lastModifiedDate
        + "]";
  }
}
