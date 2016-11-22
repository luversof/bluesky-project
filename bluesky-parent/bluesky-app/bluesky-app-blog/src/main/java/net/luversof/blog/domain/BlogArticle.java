package net.luversof.blog.domain;

import java.time.ZonedDateTime;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Data;

@Data
@Entity
@Table(name = "BlogArticle")
public class BlogArticle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@NotNull(groups = { Get.class })
	private long id;

	@ManyToOne
	@JoinColumn(name = "blog_id", foreignKey = @ForeignKey(name = "FK_blogArticle_blogId"))
	private Blog blog;

	@NotEmpty(groups = { Save.class, Modify.class })
	private String title;

	@NotEmpty(groups = { Save.class, Modify.class })
	@Column(columnDefinition="TEXT")
	private String content;

	@CreatedDate
	private ZonedDateTime createdDate;

	@LastModifiedDate
	private ZonedDateTime lastModifiedDate;

	@ManyToOne
	@JoinColumn(name = "blogArticleCategory_id", foreignKey = @ForeignKey(name = "FK_blogArticle_blogArticleCategoryId"))
	private BlogArticleCategory blogArticleCategory;

	@OneToOne(cascade = CascadeType.ALL)
	@JsonManagedReference
	private BlogArticleStatistics blogArticleStatistics;

	public interface Get {
	};

	public interface Save {
	};

	public interface Modify {
	};
}