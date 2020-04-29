package net.luversof.blog.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
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
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.Data;

@Data
@Entity
@Audited
public class BlogArticle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@NotNull(groups = { Get.class, Update.class, Delete.class, BlogComment.Update.class })
	private long id;

	@ManyToOne
	@JoinColumn(name = "blog_id", foreignKey = @ForeignKey(name = "FK_blogArticle_blogId"), nullable = false)
	@NotNull(groups = { Update.class, Delete.class })
	private Blog blog;

	@NotEmpty(groups = { Create.class, Update.class })
	@Length(min = 3, max = 50, groups = { Create.class, Update.class })
	private String title;

	@NotEmpty(groups = { Create.class, Update.class })
	// @Column(columnDefinition = "TEXT")
	@Lob
	private String content;

	@CreatedDate
	private LocalDateTime createdDate;

	@LastModifiedDate
	private LocalDateTime lastModifiedDate;
	
	@Column(name = "user_id", length = 16, nullable = false)
	private UUID userId;

	private long viewCount;

	@ManyToOne
	@JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "FK_blogArticle_categoryId"))
	private BlogArticleCategory blogArticleCategory;

	public interface Get {
	}

    public interface Create {
	}

    public interface Update {
	}

    public interface Delete {
	}
}