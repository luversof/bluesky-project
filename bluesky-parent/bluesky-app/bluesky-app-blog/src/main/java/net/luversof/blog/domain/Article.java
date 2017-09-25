package net.luversof.blog.domain;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.rest.core.annotation.Description;

import lombok.Data;

@Data
@Entity
@Audited
@Description("게시글 관련 도메인.")
public class Article {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@NotNull(groups = { Get.class })
	private long id;

	@ManyToOne
	@JoinColumn(name = "blog_id", foreignKey = @ForeignKey(name = "FK_article_blogId"))
	private Blog blog;

	@Description("제목")
	@NotEmpty(groups = { Save.class, Modify.class })
	private String title;

	@NotEmpty(groups = { Save.class, Modify.class })
	//@Column(columnDefinition = "TEXT")
	@Lob
	private String content;

	@CreatedDate
	private LocalDateTime createdDate;

	@LastModifiedDate
	private LocalDateTime lastModifiedDate;

	private long viewCount;
	
	@ManyToOne
	@JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "FK_article_categoryId"))
	private Category category;

	public interface Get {
	};

	public interface Save {
	};

	public interface Modify {
	};
}