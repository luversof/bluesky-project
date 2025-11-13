package net.luversof.api.user.domain;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

/**
 * Spring Security 의 기본 테이블과 별개로 추가적인 사용자 정보를 저장하기 위한 도메인 클래스
 */

@Data
@Table("UserInfo")
public class UserInfo {

	@Id
	private UUID id;

	private String username;

	private String password;

	/**
	 * OAuth2 Provider (github, kakao 등)
	 */
	private String provider;

	/**
	 * OAuth2 Provider의 사용자 ID
	 */
	private String providerId;

	/**
	 * 이메일
	 */
	private String email;

	/**
	 * 프로필 이미지 URL
	 */
	private String avatarUrl;
}
