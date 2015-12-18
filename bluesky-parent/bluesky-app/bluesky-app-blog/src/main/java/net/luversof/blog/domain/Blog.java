package net.luversof.blog.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(indexes = @Index(name = "IDX_BLOG_userId", columnList = "userId") )
public class Blog {

	@Id
	@GeneratedValue
	private long id;

	private long userId;

}
