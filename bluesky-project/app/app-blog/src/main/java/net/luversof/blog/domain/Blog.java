package net.luversof.blog.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.Data;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Data
public class Blog {
	@Id
	@GeneratedValue
	private long id;

	@NotEmpty(groups = Save.class)
	private String username;

	@NotEmpty(groups = Save.class)
	private String title;

	@NotEmpty(groups = Save.class)
	private String content;

	@CreatedDate
	@Column(updatable=false)
	private Date createdDate;

	@LastModifiedDate
	private Date lastModifiedDate;

	@OneToOne
	private BlogCategory blogCategory;
	
	
	public interface Save {};
}