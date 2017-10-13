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
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.rest.core.annotation.Description;

import lombok.Data;

@Data
@Entity
@Audited
public class Article {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@NotNull(groups = { Get.class, Save.class, Delete.class })
	private long id;

	@ManyToOne
	@JoinColumn(name = "blog_id", foreignKey = @ForeignKey(name = "FK_article_blogId"))
	@NotNull(groups = { Save.class, Delete.class })
	private Blog blog;

	@Description("제목")
	@NotEmpty(groups = { Create.class, Save.class })
	private String title;

	@NotEmpty(groups = { Create.class, Save.class })
	// @Column(columnDefinition = "TEXT")
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

	public interface Create {
	};

	public interface Save {
	};

	public interface Delete {
	};
}