package net.luversof.api.user.domain;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

/**
 * Spring Security 의 기본 테이블과 별개로 추가적인 사용자 정보를 저장하기 위한 도메인 클래스
 */

@Data
@Table("UserInfo")
public class UserInfo {

	@Id
	@Column("id")
	private UUID id;

	private String username;

	private String password;

}
