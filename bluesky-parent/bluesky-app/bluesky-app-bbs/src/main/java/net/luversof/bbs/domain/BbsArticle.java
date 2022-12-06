package net.luversof.bbs.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

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
@Table(indexes = { @Index(name = "UK_bbsArticle_bbsArticleId", columnList = "bbsArticleId", unique = true), @Index(name = "IDX_article_bbsId", columnList = "bbs_id"), @Index(name = "IDX_article_userId", columnList = "user_id") })
public class BbsArticle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@NotNull(groups = { Get.class })
	private long id;

	@Column(length = 36, nullable = false)
	private String bbsArticleId;

	@Column(name = "user_id", length = 36, nullable = false)
	private String userId;

	@Column(name = "bbs_id", length = 36, nullable = false)
	private String bbsId;

	@NotBlank(groups = { Save.class, Modify.class })
	private String title;

	@NotBlank(groups = { Save.class, Modify.class })
	private String content;

	@Column(updatable = false)
	@CreatedDate
	private LocalDateTime createdDate;

	@Column(updatable = false)
	@LastModifiedDate
	private LocalDateTime lastModifiedDate;

	public interface Get {
	}

	public interface Save {
	}

	public interface Modify {
	}
}