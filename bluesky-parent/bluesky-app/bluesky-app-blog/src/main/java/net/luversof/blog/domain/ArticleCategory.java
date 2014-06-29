package net.luversof.blog.domain;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import lombok.Data;

@Entity
@Data
public class ArticleCategory {
	@Id
	@GeneratedValue
	private long id;

	@OneToOne
	private Blog blog;

	private String name;

	@ManyToOne(cascade = { CascadeType.ALL })
	@JoinColumn(name = "upperMenu_id")
	private ArticleCategory upperCategory;

	@OneToMany(mappedBy = "upperCategory", fetch = FetchType.EAGER)
	private List<ArticleCategory> lowerCategories;
}