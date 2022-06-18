package net.luversof.user.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Entity
@Data
@Table(name = "User", indexes = { @Index(name = "UK_user_userId", columnList = "userId", unique = true), @Index(name = "UK_user_userNameUserType", columnList = "userName, userType", unique = true) })
public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(length = 16)
	private long idx;
	
	@NotBlank(groups = Create.class)
	@Column(nullable = false, length = 36)
	private String userId;

	@NotBlank(groups = Create.class)
	@Column(nullable = false)
	private String userName;

	@JsonIgnore
	private String password;

	@JsonIgnore
	@CreationTimestamp
	private ZonedDateTime createdDate;

	@JsonIgnore
	private boolean accountNonExpired;
	
	@JsonIgnore
    private boolean accountNonLocked;
	
	@JsonIgnore
    private boolean credentialsNonExpired;
	
	@JsonIgnore
    private boolean enabled;

	@OneToMany
	@JoinColumn(name = "user_id", referencedColumnName = "userId")
	private List<UserAuthority> userAuthorityList;
	
	@JsonIgnore
	@Enumerated(EnumType.STRING)
	private UserType userType;
	
	@JsonIgnore
	private String externalId;
	
	public interface Create {}
}
