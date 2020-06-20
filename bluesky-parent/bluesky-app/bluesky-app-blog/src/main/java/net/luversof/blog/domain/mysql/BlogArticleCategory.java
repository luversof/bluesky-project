package net.luversof.blog.domain.mysql;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

import org.hibernate.envers.Audited;

import lombok.Data;

@Data
@Entity
@Audited
public class BlogArticleCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Min(value = 1, groups = { Update.class, Delete.class})
	private long id;

	@ManyToOne
	@JoinColumn(name = "blog_id", foreignKey = @ForeignKey(name = "FK_blogArticleCategory_blogId"), nullable = false)
	private Blog blog;

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