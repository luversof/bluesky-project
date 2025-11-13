package net.luversof.web.gate.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;

/**
 * GitHub OAuth 로그인 성공 후 UserInfo 저장 처리
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	@Setter(onMethod_ = @Autowired)
	private OAuth2AuthorizedClientRepository authorizedClientRepository;

	@Setter(onMethod_ = @Autowired)
	private net.luversof.web.gate.user.openfeign.UserApiClient userApiClient;

	public OAuth2LoginSuccessHandler() {
		setDefaultTargetUrl("/");
		setAlwaysUseDefaultTargetUrl(true);
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		System.out.println("=== OAuth2LoginSuccessHandler.onAuthenticationSuccess 호출됨 ===");
		System.out.println("Authentication type: " + authentication.getClass().getName());

		if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
			String registrationId = oauthToken.getAuthorizedClientRegistrationId();
			String provider = normalizeProvider(registrationId);
			System.out.println("Provider: " + registrationId + " (normalized: " + provider + ")");

			// GitHub/Kakao에서 받은 OAuth2AuthorizedClient 로드
			OAuth2AuthorizedClient authorizedClient = authorizedClientRepository.loadAuthorizedClient(
				registrationId,
				authentication,
				request);

			System.out.println("AuthorizedClient: " + (authorizedClient != null ? "존재" : "null"));

			if (authorizedClient != null) {
				// GitHub 사용자 정보를 UserInfo 테이블에 저장
				System.out.println("=== UserInfo 저장 시작 (provider: " + provider + ") ===");
				saveUserInfo(oauthToken, provider);
				System.out.println("=== UserInfo 저장 완료 ===");

				// TODO: Token Exchange는 필요시 별도 엔드포인트에서 호출
				// 로그인 성공 시에는 UserInfo 저장만 수행
			}
		}

		super.onAuthenticationSuccess(request, response, authentication);
	}

	private void saveUserInfo(OAuth2AuthenticationToken oauthToken, String provider) {
		try {
			var principal = oauthToken.getPrincipal();
			
			// GitHub OAuth2User 속성에서 사용자 정보 추출
			Object idAttr = principal.getAttribute("id");
			String providerId = idAttr != null ? idAttr.toString() : null;
			String username = principal.getAttribute("login");
			String email = principal.getAttribute("email");
			String avatarUrl = principal.getAttribute("avatar_url");
			
			System.out.println("=== GitHub 사용자 정보 추출 ===");
			System.out.println("  - provider: " + provider);
			System.out.println("  - providerId: " + providerId);
			System.out.println("  - username (login): " + username);
			System.out.println("  - email: " + email);
			System.out.println("  - avatarUrl: " + avatarUrl);
			
			// username이 null인 경우 체크
			if (username == null || username.trim().isEmpty()) {
				System.err.println("경고: GitHub login 속성이 null입니다. OAuth2User 전체 속성:");
				principal.getAttributes().forEach((key, value) -> 
					System.out.println("    " + key + " = " + value));
				return;
			}
			
			// api-user에 사용자 정보 저장하고 UserInfo ID 반환 받기
			var request = new net.luversof.web.gate.user.openfeign.UserApiClient.SaveOAuth2UserRequest(
				provider,
				providerId,
				username,
				email,
				avatarUrl
			);
			
			var userInfo = userApiClient.saveOAuth2User(request);
			if (userInfo != null && userInfo.id() != null) {
				System.out.println("UserInfo 저장 성공! ID: " + userInfo.id());
			} else {
				System.err.println("UserInfo 저장 실패: ID가 null");
			}
		} catch (Exception e) {
			System.err.println("UserInfo 저장 중 에러 발생: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Provider 이름을 정규화 (github-local → github)
	 */
	private String normalizeProvider(String provider) {
		if (provider == null) {
			return null;
		}
		// github-local, github-dev 등을 모두 github로 통일
		if (provider.startsWith("github")) {
			return "github";
		}
		// kakao-local, kakao-dev 등을 모두 kakao로 통일
		if (provider.startsWith("kakao")) {
			return "kakao";
		}
		return provider;
	}
}
