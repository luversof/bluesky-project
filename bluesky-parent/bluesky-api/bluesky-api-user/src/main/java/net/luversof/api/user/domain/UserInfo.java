package net.luversof.api.user.domain;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Spring Security 의 기본 테이블과 별개로 추가적인 사용자 정보를 저장하기 위한 도메인 클래스
 */

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

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserInfo other = (UserInfo) obj;
		return Objects.equals(avatarUrl, other.avatarUrl) && Objects.equals(email, other.email)
				&& Objects.equals(id, other.id) && Objects.equals(password, other.password)
				&& Objects.equals(provider, other.provider) && Objects.equals(providerId, other.providerId)
				&& Objects.equals(username, other.username);
	}

	@Override
	public int hashCode() {
		return Objects.hash(avatarUrl, email, id, password, provider, providerId, username);
	}

	@Override
	public String toString() {
		return "UserInfo [id=" + id + ", username=" + username + ", password=" + password + ", provider=" + provider
				+ ", providerId=" + providerId + ", email=" + email + ", avatarUrl=" + avatarUrl + "]";
	}
}
