package net.luversof.blog.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import lombok.Data;

import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Entity
@Data
public class Article {
	@Id
	@GeneratedValue
	@NotNull(groups = { Get.class })
	private long id;
	
	@OneToOne
	private Blog blog;

	@NotEmpty(groups = { Save.class, Modify.class })
	private String title;

	@NotEmpty(groups = { Save.class, Modify.class })
	private String content;

	@Column(updatable = false)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@CreatedDate
	private LocalDateTime createdDate;

	@Column(updatable = false)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	@LastModifiedDate
	private LocalDateTime lastModifiedDate;

	@OneToOne
	private ArticleCategory articleCategory;

	public interface Get {
	};

	public interface Save {
	};

	public interface Modify {
	};
}