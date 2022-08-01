package net.luversof.blog.domain.mysql;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Length;

import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "UK_blogArticle_blogArticleId", columnList = "blogArticleId", unique = true), @Index(name = "IDX_blogArticle_blogId", columnList = "blog_id"), @Index(name = "IDX_blogArticle_userId", columnList = "user_id") })
public class BlogArticle implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long idx;
	
	@NotBlank(groups = { Get.class, Update.class, UpdateParam.class, Delete.class, DeleteParam.class })
	@Column(length = 36, nullable = false)
	private String blogArticleId;

	@NotBlank(groups = { Create.class })
	@Column(name = "blog_id", length = 36, nullable = false)
	private String blogId;
	
	@OneToOne
	@JoinColumn(name = "blogArticleCategory_id", referencedColumnName = "blogArticleCategoryId")
	private BlogArticleCategory blogArticleCategory;
	
	@OneToMany(mappedBy = "blogArticleId")
	private List<BlogArticleComment> blogArticleCommentList;

	@NotBlank(groups = { Create.class, CreateParam.class, Update.class })
	@Length(min = 3, max = 50, groups = { Create.class, CreateParam.class, Update.class, UpdateParam.class })
	private String title;

	@NotBlank(groups = { Create.class, CreateParam.class, Update.class, UpdateParam.class })
	// @Column(columnDefinition = "TEXT")
	@Lob
	private String content;

	@CreationTimestamp
	private ZonedDateTime createdDate;

	@UpdateTimestamp
	private ZonedDateTime lastModifiedDate;
	
	@NotBlank(groups = { Create.class, Update.class, Delete.class })
	@Column(name = "user_id", length = 36, nullable = false)
	private String userId;

	public interface Get {
	}

    public interface Create {
	}
    
    public interface CreateParam {
	}

    public interface Update {
	}
    
    public interface UpdateParam {
	}

    public interface Delete {
	}
    
    public interface DeleteParam {
	}
}