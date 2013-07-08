package net.luversof.blog.domain;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.Data;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Data
public class BlogPost {
	@Id
	@GeneratedValue
	private long id;

	private long memberId;

	private String title;

	private String content;

	// @Column(columnDefinition = "timestamp")
	// @Temporal(TemporalType.TIMESTAMP)
	// @DateTimeFormat(iso = ISO.DATE)
	@CreatedDate
	private Date createdDate;

	@LastModifiedDate
	private Date lastModifiedDate;

	@OneToOne
	private BlogCategory blogCategory;

}