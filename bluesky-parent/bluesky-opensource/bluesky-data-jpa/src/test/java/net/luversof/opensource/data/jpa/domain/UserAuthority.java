package net.luversof.opensource.data.jpa.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Data;
import lombok.ToString;

@Entity
@Data
@ToString(exclude = "user")
public class UserAuthority {

	@Id
	@GeneratedValue
	private long id;

	@ManyToOne
//	@JoinColumn(name = "userId")
	private User user;

	private String authority;
}
