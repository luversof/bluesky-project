package net.luversof.blog.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import lombok.Data;

import org.hibernate.annotations.Columns;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

@Entity
@Data
public class Article {
	@Id
	@GeneratedValue
	@NotNull(groups = { Get.class })
	private long articleId;
	
	@OneToOne
	@JoinColumn(name = "blogId")
	private Blog blog;

	@NotEmpty(groups = { Save.class, Modify.class })
	private String title;

	@NotEmpty(groups = { Save.class, Modify.class })
	private String content;

	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@Column(updatable = false)
	@CreatedDate
	private LocalDateTime createdDate;

	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@Column(updatable = false)
	@LastModifiedDate
	private LocalDateTime lastModifiedDate;

	@OneToOne
	@JoinColumn(name = "articleCategoryId")
	private ArticleCategory articleCategory;

	public interface Get {
	};

	public interface Save {
	};

	public interface Modify {
	};
}