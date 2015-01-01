package net.luversof.opensource.data.jpa.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Blog {
	@Id
	@GeneratedValue
	private long id;
	
	private long userId;
	
	private String userType;
}
