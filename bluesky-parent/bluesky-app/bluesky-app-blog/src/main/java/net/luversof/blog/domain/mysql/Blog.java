package net.luversof.blog.domain.mysql;

import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Data;

/**
 * blog 정보
 * @author luver
 *
 */
@Data
@Entity
@Table(indexes = { @Index(columnList = "user_id") })
public class Blog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(length = 16)
	private long idx;
	
	@Column(length = 36, nullable = false, unique = true)
	private String blogId;

	@Column(name = "user_id", length = 36, nullable = false)
	private String userId;
	
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "blogId")
	private List<BlogArticleCategory> blogArticleCategoryList;
	
	@CreationTimestamp
	private ZonedDateTime createdDate;

}
