package net.luversof.blog.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.ForeignKey;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "BlogArticleStatistics")
@Data
@ToString(exclude = "blogArticle")
@EqualsAndHashCode(exclude = "blogArticle")
public class BlogArticleStatistics {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@OneToOne
	@JsonBackReference
//	@ApiModelProperty(hidden = true)
	@JoinColumn(name = "blogArticle_id", unique = true, foreignKey = @ForeignKey(name = "FK_blogArticleCategory_blogArticleId"))
	private BlogArticle blogArticle;
	
	private long viewCount;
//	private int commentCount;	// 이거 쓸일 없어보이네
}
