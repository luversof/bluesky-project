package net.luversof.api.blog.domain.mariadb;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "UK_blogArticle_blogArticleId", columnList = "blogArticleId", unique = true), @Index(name = "IDX_blogArticle_blogId", columnList = "blog_id"), @Index(name = "IDX_blogArticle_userId", columnList = "user_id") })
public class BlogArticle implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long idx;
	
	@NotBlank(groups = { Get.class, Update.class, Delete.class })
	@Column(length = 36, nullable = false)
	private String blogArticleId;

	@NotBlank(groups = { Create.class })
	@Column(name = "blog_id", length = 36, nullable = false)
	private String blogId;
	
	@NotBlank(groups = { Create.class, Update.class, Delete.class })
	@Column(name = "user_id", length = 36, nullable = false)
	private String userId;
	
	@OneToOne
	@JoinColumn(name = "blogArticleCategory_id", referencedColumnName = "blogArticleCategoryId")
	private BlogArticleCategory blogArticleCategory;
	
	@OneToMany(mappedBy = "blogArticleId")
	private List<BlogArticleComment> blogArticleCommentList;

	@NotBlank(groups = { Create.class, Update.class })
	@Size(min = 3, max = 50, groups = { Create.class, Update.class })
	private String title;

	@NotBlank(groups = { Create.class, Update.class })
	// @Column(columnDefinition = "TEXT")
	@Lob
	private String content;

	@CreationTimestamp
	private ZonedDateTime createdDate;

	@UpdateTimestamp
	private ZonedDateTime lastModifiedDate;

	public interface Create {
	}

	public interface Get {
	}
    
    public interface Update {
	}

    public interface Delete {
	}
    
}