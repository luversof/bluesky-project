package net.luversof.security.domain;

import javax.persistence.Id;
import javax.persistence.ManyToOne;

public class UserAuthority {
	
	@Id
	private long id;
	
	@ManyToOne
	private User user;

	private String authority;
}
