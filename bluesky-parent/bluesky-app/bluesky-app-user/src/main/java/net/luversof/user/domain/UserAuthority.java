package net.luversof.user.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(indexes = { @Index(name = "IDX_userAuthority_userId", columnList = "user_id") })	// db 에서 create index 수행 시 table name이 소문자로 바뀜 (개인 db 설정 문제인 듯.)
public class UserAuthority {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long idx;
	
	@Column(name = "user_id",  nullable = false, length = 36)
	private String userId;

	@Column(nullable = false)
	private String authority;
}
