package net.luversof.blog.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import lombok.Data;

import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Data
public class Blog {
	@Id
	@GeneratedValue
	@NotNull(groups = { Get.class })
	private long id;

	private String username;

	@NotEmpty(groups = { Save.class, Modify.class })
	private String title;

	@NotEmpty(groups = { Save.class, Modify.class })
	private String content;

	@Type(type="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	@CreatedDate
	@Column(updatable = false)
	private DateTime createdDate;

	@LastModifiedDate
	private Date lastModifiedDate;

	@OneToOne
	private BlogCategory blogCategory;

	public interface Get {
	};

	public interface Save {
	};

	public interface Modify {
	};
}