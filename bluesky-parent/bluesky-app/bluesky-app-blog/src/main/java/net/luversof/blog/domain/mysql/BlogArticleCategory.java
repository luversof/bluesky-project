package net.luversof.blog.domain.mysql;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

import lombok.Data;

@Data
@Entity
//@Table(indexes = @Index(columnList = "blog_id"))
public class BlogArticleCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Min(value = 1, groups = { Update.class, Delete.class})
	private long idx;
	
	@Column(length = 36, nullable = false)
	private String blogArticleCategoryId;

	@Column(name = "blog_id", length = 36, nullable = false)
	private String blogId;

	@Column(length = 64, nullable = false)
	@NotEmpty(groups = { Create.class, Update.class })
	private String name;

	public interface Create {
	}

	public interface Update {
	}

	public interface Delete {
	}
}