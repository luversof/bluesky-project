package net.luversof.api.blog.domain.mariadb;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "UK_blogArticleCategory_blogArticleCategoryId", columnList = "blogArticleCategoryId", unique = true), @Index(name = "IDX_blogArticleCategory_blogId", columnList = "blog_id") })
public class BlogArticleCategory implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long idx;
	
	@NotBlank(groups = Update.class)
	@Column(length = 36, nullable = false)
	private String blogArticleCategoryId;

	@NotBlank(groups = Create.class)
	@Column(name = "blog_id", length = 36, nullable = false)
	private String blogId;

	@NotBlank(groups = { Create.class, Update.class })
	@Column(length = 64, nullable = false)
	private String name;

	public interface Create {}

	public interface Update {}
	
	public interface Delete {}

}