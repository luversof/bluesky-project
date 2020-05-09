package net.luversof.user.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Entity
@Data
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "username", "userType" }))
public class User {
	@Id
	//@GeneratedValue(strategy = GenerationType.IDENTITY)
	@GeneratedValue(generator = "uuid-gen")
	@GenericGenerator(name = "uuid-gen", strategy = "uuid2")
	@Column(length = 16)
	private UUID id;

	@Column(nullable = false)
	private String username;

	@JsonIgnore
	private String password;

	@JsonIgnore
	@CreatedDate
//	@DateTimeFormat(iso = ISO.DATE_TIME)
	private LocalDateTime createdDate;

	@JsonIgnore
	private boolean accountNonExpired;
	
	@JsonIgnore
    private boolean accountNonLocked;
	
	@JsonIgnore
    private boolean credentialsNonExpired;
	
	@JsonIgnore
    private boolean enabled;

	@JsonIgnore
    // @JsonManagedReference
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "user")
	private List<UserAuthority> userAuthorityList;
	
	@JsonIgnore
	@Enumerated(EnumType.STRING)
	private UserType userType;
	
	@JsonIgnore
	private String externalId;
}
