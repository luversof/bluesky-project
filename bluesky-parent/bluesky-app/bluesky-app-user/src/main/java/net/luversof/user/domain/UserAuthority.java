package net.luversof.user.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "IDX_userAuthority_userId", columnList = "user_id") })	// db 에서 create index 수행 시 table name이 소문자로 바뀜 (개인 db 설정 문제인 듯.)
public class UserAuthority implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long idx;
	
	@Column(name = "user_id",  nullable = false, length = 36)
	private String userId;

	@Column(nullable = false)
	private String authority;
}
