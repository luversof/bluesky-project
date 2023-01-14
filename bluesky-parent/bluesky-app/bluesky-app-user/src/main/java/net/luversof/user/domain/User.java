package net.luversof.user.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "UK_user_userId", columnList = "userId", unique = true), @Index(name = "UK_user_userNameUserType", columnList = "userName, userType", unique = true) })
public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long idx;
	
	@NotBlank(groups = { Create.class, Delete.class })
	@Column(nullable = false, length = 36)
	private String userId;

	@NotBlank(groups = Create.class)
	@Column(nullable = false)
	private String userName;

	private String password;

	@CreationTimestamp
	private ZonedDateTime createdDate;

	private boolean accountNonExpired;
	
    private boolean accountNonLocked;
	
    private boolean credentialsNonExpired;
	
    private boolean enabled;

	@OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<UserAuthority> userAuthorityList;
	
	@Enumerated(EnumType.STRING)
	private UserType userType;
	
	private String externalId;
	
	public interface Create {}
	
	public interface Delete {}
}
