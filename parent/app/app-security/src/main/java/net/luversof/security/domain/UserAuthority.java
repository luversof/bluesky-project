package net.luversof.security.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Data;

@Entity
@Data
public class UserAuthority {
	
	@Id
	private long id;
	
	@ManyToOne
	private User user;

	private String authority;
}
