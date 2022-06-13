package net.luversof.blog.domain.mysql;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;

import lombok.Data;

@Data
@Entity
@Table(name = "BlogArticleCategory", indexes = @Index(columnList = "blog_id"))
public class BlogArticleCategory implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long idx;
	
	@Column(length = 36, nullable = false, unique = true)
	@NotEmpty(groups = { Update.class, Delete.class })
	private String blogArticleCategoryId;

	@NotEmpty(groups = Create.class)
	@Column(name = "blog_id", length = 36, nullable = false)
	private String blogId;

	@Column(length = 64, nullable = false)
	@NotEmpty(groups = { Create.class, CreateParam.class, Update.class })
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