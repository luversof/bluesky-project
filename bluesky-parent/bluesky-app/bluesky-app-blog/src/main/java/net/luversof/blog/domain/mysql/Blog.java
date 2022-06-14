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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Data;

/**
 * blog 정보
 * @author luver
 *
 */
@Data
@Entity
@Table(name = "Blog", indexes = { @Index(columnList = "user_id") })
public class Blog implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(length = 16)
	private long idx;
	
	@Column(length = 36, nullable = false, unique = true)
	private String blogId;

	@NotBlank(groups = Create.class)
	@Column(name = "user_id", length = 36, nullable = false)
	private String userId;
	
	@OneToMany
	@JoinColumn(name = "blog_id", referencedColumnName = "blogId")
	private List<BlogArticleCategory> blogArticleCategoryList;
	
	@CreationTimestamp
	private ZonedDateTime createdDate;

	
	public interface Create {};
	
}
