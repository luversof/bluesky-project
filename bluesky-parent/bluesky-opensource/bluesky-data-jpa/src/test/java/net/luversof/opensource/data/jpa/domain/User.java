package net.luversof.opensource.data.jpa.domain;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.Data;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Entity
@Data
public class User {
	@Id
	@GeneratedValue
	private long id;

	@Column(unique = true, nullable = false)
	private String username;

	private String password;

	@CreatedDate
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private LocalDateTime createdDate;

	private boolean enable;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "user")
	private List<UserAuthority> userAuthorityList;
}
