package net.luversof.security.domain;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.Data;

@Data
public class User {
	@Id
	private long id;
	
	@Column(unique=true,nullable=false)
	private String username;
	
	private String password;
	
	private boolean enable;
	
	@OneToMany(cascade=CascadeType.ALL)
	private List<UserAuthority> userAuthorityList; 
}
