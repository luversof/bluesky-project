package net.luversof.api.board.domain;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.github.f4b6a3.uuid.alt.GUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "UK_boardArticle_boardArticleId", columnList = "boardArticleId", unique = true), @Index(name = "IDX_article_bbsId", columnList = "board_id"), @Index(name = "IDX_article_userId", columnList = "user_id") })
public class BoardArticle {

	@Id
	@Null(groups = Create.class)
	@NotNull(groups = { Get.class })
	private UUID id;

	@NotBlank(groups = { Create.class, Modify.class, Delete.class })
	@Column(name = "user_id", length = 36, nullable = false)
	private String userId;

	@Column(name = "board_id", nullable = false)
	private UUID boardId;

	@NotBlank(groups = { Create.class, Modify.class })
	private String title;

	@NotBlank(groups = { Create.class, Modify.class })
	private String content;

	@Column(updatable = false)
	@CreationTimestamp
	private ZonedDateTime createdDate;

	@Column
	@UpdateTimestamp
	private ZonedDateTime lastModifiedDate;
	
	@PrePersist
    public void prePersist() {
    	id = GUID.v7().toUUID();
    }

	public interface Create {}

	public interface Get {}

	public interface Modify {}
	
	public interface Delete {}
}