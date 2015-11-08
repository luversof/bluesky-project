package net.luversof.blog.domain;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import lombok.Data;

@Entity
@Data
public class ArticleCategory {

	@Id
	@GeneratedValue
	private long id;

	@ManyToOne
	@JoinColumn(name = "blog_id", foreignKey = @ForeignKey(name = "FK_articleCategory_blogId"))
	private Blog blog;
	
	private String name;
}