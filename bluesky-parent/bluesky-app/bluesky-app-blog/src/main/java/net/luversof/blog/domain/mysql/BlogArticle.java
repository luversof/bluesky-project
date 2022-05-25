package net.luversof.blog.domain.mysql;

import java.time.ZonedDateTime;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Length;

import lombok.Data;

@Data
@Entity
public class BlogArticle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Min(value = 1, groups = { Get.class, Update.class, Delete.class, BlogComment.Update.class })
	private long id;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "blog_id", referencedColumnName = "blogId")
	private Blog blog;
	
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "category_id", referencedColumnName = "categoryId")
	private BlogArticleCategory blogArticleCategory;

	@NotEmpty(groups = { Create.class, Update.class })
	@Length(min = 3, max = 50, groups = { Create.class, Update.class })
	private String title;

	@NotEmpty(groups = { Create.class, Update.class })
	// @Column(columnDefinition = "TEXT")
	@Lob
	private String content;

	@CreationTimestamp
	private ZonedDateTime createdDate;

	@UpdateTimestamp
	private ZonedDateTime lastModifiedDate;
	
	@Column(length = 36, nullable = false)
	private String userId;

	private long viewCount;
	
	private long blogCommentCount;

	public interface Get {
	}

    public interface Create {
	}

    public interface Update {
	}

    public interface Delete {
	}
}