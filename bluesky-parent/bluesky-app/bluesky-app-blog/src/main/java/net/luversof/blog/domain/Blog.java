package net.luversof.blog.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Blog {
	@Id
	@GeneratedValue
	private long blogId;
	
	private long userId;
	
	private String userType;
}
