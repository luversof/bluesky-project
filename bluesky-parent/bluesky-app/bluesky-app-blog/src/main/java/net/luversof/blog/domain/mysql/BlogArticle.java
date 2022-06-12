package net.luversof.blog.domain.mysql;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Length;

import lombok.Data;

@Data
@Entity
public class BlogArticle implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Min(value = 1, groups = { Get.class, Update.class, Delete.class, BlogArticleComment.Update.class })
	private long idx;
	
	@Column(length = 36, nullable = false, unique = true)
	private String blogArticleId;

	@NotEmpty(groups = { Create.class })
	@Column(name = "blog_id", length = 36, nullable = false)
	private String blogId;
	
	@OneToOne
	@JoinColumn(name = "blogArticleCategory_id", referencedColumnName = "blogArticleCategoryId")
	private BlogArticleCategory blogArticleCategory;
	
	@OneToMany
	@JoinColumn(name = "blogArticle_id", referencedColumnName = "blogArticleId")
	private List<BlogArticleComment> blogArticleCommentList;

	@NotEmpty(groups = { Create.class, CreateRequest.class, Update.class })
	@Length(min = 3, max = 50, groups = { Create.class, CreateRequest.class, Update.class })
	private String title;

	@NotEmpty(groups = { Create.class, CreateRequest.class, Update.class })
	// @Column(columnDefinition = "TEXT")
	@Lob
	private String content;

	@CreationTimestamp
	private ZonedDateTime createdDate;

	@UpdateTimestamp
	private ZonedDateTime lastModifiedDate;
	
	@NotEmpty(groups = { Create.class })
	@Column(name = "user_id", length = 36, nullable = false)
	private String userId;

	public interface Get {
	}

    public interface Create {
	}
    
    public interface CreateRequest {
	}

    public interface Update {
	}

    public interface Delete {
	}
}