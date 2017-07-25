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

import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;

import lombok.Data;

@Entity
@Data
public class User {
	@Id
	//@GeneratedValue(strategy = GenerationType.IDENTITY)
	@GeneratedValue(generator = "uuid-gen")
	@GenericGenerator(name = "uuid-gen", strategy = "uuid2")
	@Column(length = 16)
	private UUID id;

	@Column(nullable = false)
	private String username;

	private String password;

	@CreatedDate
//	@DateTimeFormat(iso = ISO.DATE_TIME)
	private LocalDateTime createdDate;

	private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "user")
	private List<UserAuthority> userAuthorityList;
	
	@Enumerated(EnumType.STRING)
	private UserType userType;
	
	private String externalId;
}
