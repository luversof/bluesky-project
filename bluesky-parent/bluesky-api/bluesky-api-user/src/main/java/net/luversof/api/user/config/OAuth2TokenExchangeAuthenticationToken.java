package net.luversof.api.user.config;

import java.util.Collections;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.util.Assert;

/**
 * Token Exchange Grant Type을 위한 Authentication Token
 */
public class OAuth2TokenExchangeAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

	private static final long serialVersionUID = 1L;

	private static final AuthorizationGrantType TOKEN_EXCHANGE_GRANT_TYPE = 
			new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:token-exchange");

	private final String subjectToken;
	private final String subjectTokenType;

	/**
	 * Token Exchange 인증 토큰 생성
	 * 
	 * @param clientPrincipal 클라이언트 인증 정보
	 * @param subjectToken 교환할 토큰 (GitHub access token)
	 * @param subjectTokenType 토큰 타입
	 * @param additionalParameters 추가 파라미터
	 */
	public OAuth2TokenExchangeAuthenticationToken(
			Authentication clientPrincipal,
			String subjectToken,
			String subjectTokenType,
			@Nullable Map<String, Object> additionalParameters) {
		super(TOKEN_EXCHANGE_GRANT_TYPE, clientPrincipal, 
				additionalParameters != null ? additionalParameters : Collections.emptyMap());
		Assert.hasText(subjectToken, "subjectToken cannot be empty");
		Assert.hasText(subjectTokenType, "subjectTokenType cannot be empty");
		this.subjectToken = subjectToken;
		this.subjectTokenType = subjectTokenType;
	}

	public String getSubjectToken() {
		return this.subjectToken;
	}

	public String getSubjectTokenType() {
		return this.subjectTokenType;
	}
}
