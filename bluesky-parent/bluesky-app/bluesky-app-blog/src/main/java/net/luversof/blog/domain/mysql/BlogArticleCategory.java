package net.luversof.blog.domain.mysql;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
@Entity
@Table(name = "BlogArticleCategory", indexes = @Index(columnList = "blog_id"))
public class BlogArticleCategory implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long idx;
	
	@NotBlank(groups = { Update.class, Delete.class })
	@Column(length = 36, nullable = false, unique = true)
	private String blogArticleCategoryId;

	@NotBlank(groups = Create.class)
	@Column(name = "blog_id", length = 36, nullable = false)
	private String blogId;

	@NotBlank(groups = { Create.class, CreateParam.class, Update.class })
	@Column(length = 64, nullable = false)
	private String name;

	public interface Create {
	}
	
	public interface CreateParam {
	}

	public interface Update {
	}

	public interface Delete {
	}
}