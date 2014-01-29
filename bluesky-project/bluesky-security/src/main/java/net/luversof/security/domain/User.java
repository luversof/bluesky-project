package net.luversof.security.domain;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import lombok.Data;

@Entity
@Data
public class User {
	@Id
	private long id;
	
	@Column(unique=true,nullable=false)
	private String username;
	
	private String password;
	
	private boolean enable;
	
	@OneToMany(fetch=FetchType.EAGER)
	@JoinColumn(name="user_id", referencedColumnName="id")
	private List<UserAuthority> userAuthorityList; 
}
