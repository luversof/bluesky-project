package net.luversof.user.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Entity
@Data
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "userName", "userType" }) })
public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(length = 16)
	private long idx;
	
	@Column(nullable = false, length = 36, unique = true)
	private String userId;

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

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "user_id", referencedColumnName = "userId")
	private List<UserAuthority> userAuthorityList;
	
	@JsonIgnore
	@Enumerated(EnumType.STRING)
	private UserType userType;
	
	@JsonIgnore
	private String externalId;
}
