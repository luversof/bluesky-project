package net.luversof.blog.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@ToString(exclude = "article")
@EqualsAndHashCode(exclude = "article")
public class ArticleStatistics {

	@Id
	@GeneratedValue
	private int id;
	
	@OneToOne
	@JsonBackReference
//	@ApiModelProperty(hidden = true)
	private Article article;
	
	private int viewCount;
//	private int commentCount;	// 이거 쓸일 없어보이네
}
