package net.luversof.blog.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import lombok.Data;

@Entity
@Data
public class ArticleCategory {
	@Id
	@GeneratedValue
	private long articleCategoryId;

	@OneToOne
	@JoinColumn(name = "blogId")
	private Blog blog;

	private String name;
}