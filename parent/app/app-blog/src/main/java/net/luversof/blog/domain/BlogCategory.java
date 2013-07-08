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

import lombok.Data;

@Entity
@Data
public class BlogCategory {
	@Id
	@GeneratedValue
	private long id;

	private long memberId;

	private String title;

	@ManyToOne(cascade = { CascadeType.ALL })
	@JoinColumn(name = "upperMenu_id")
	private BlogCategory upperCategory;

	@OneToMany(mappedBy = "upperCategory", fetch = FetchType.EAGER)
	private List<BlogCategory> lowerCategories;
}