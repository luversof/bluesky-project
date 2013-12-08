package net.luversof.blog.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.Data;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Data
public class Blog {
	@Id
	@GeneratedValue
	private long id;

	private String username;

	private String title;

	private String content;

	// @Column(columnDefinition = "timestamp")
	// @Temporal(TemporalType.TIMESTAMP)
	// @DateTimeFormat(iso = ISO.DATE)
	@CreatedDate
	@Column(updatable=false)
	private Date createdDate;

	@LastModifiedDate
	private Date lastModifiedDate;

	@OneToOne
	private BlogCategory blogCategory;
	
	
	public interface Save {};
}