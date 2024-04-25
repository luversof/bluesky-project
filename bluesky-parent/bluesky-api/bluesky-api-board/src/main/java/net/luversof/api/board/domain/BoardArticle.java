package net.luversof.api.board.domain;

import java.time.ZonedDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "UK_boardArticle_boardArticleId", columnList = "boardArticleId", unique = true), @Index(name = "IDX_article_bbsId", columnList = "board_id"), @Index(name = "IDX_article_userId", columnList = "user_id") })
public class BoardArticle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@NotNull(groups = { Get.class })
	private long id;

	@NotBlank(groups = Delete.class)
	@Column(length = 36, nullable = false)
	private String boardArticleId;

	@NotBlank(groups = { Create.class, Modify.class, Delete.class })
	@Column(name = "user_id", length = 36, nullable = false)
	private String userId;

	@Column(name = "board_id", length = 36, nullable = false)
	private String boardId;

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

	public interface Create {}

	public interface Get {}

	public interface Modify {}
	
	public interface Delete {}
}