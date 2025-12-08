package net.luversof.client.user.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import net.luversof.client.user.httpexchange.UserInfoApiClient;

/**
 * Common OAuth2 Login Success Handler for all bluesky-web modules
 * Saves user information to bluesky-api-user after successful OAuth2 login
 */
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	@Setter(onMethod_ = @Autowired)
	private OAuth2AuthorizedClientRepository authorizedClientRepository;

	@Setter(onMethod_ = @Autowired)
	private UserInfoApiClient userInfoApiClient;

	public OAuth2LoginSuccessHandler() {
		setDefaultTargetUrl("/");
		setAlwaysUseDefaultTargetUrl(true);
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
			String registrationId = oauthToken.getAuthorizedClientRegistrationId();
			String provider = normalizeProvider(registrationId);

			OAuth2AuthorizedClient authorizedClient = authorizedClientRepository.loadAuthorizedClient(
				registrationId,
				authentication,
				request);

			if (authorizedClient != null) {
				saveUserInfo(oauthToken, provider);
			}
		}

		super.onAuthenticationSuccess(request, response, authentication);
	}

	private void saveUserInfo(OAuth2AuthenticationToken oauthToken, String provider) {
		try {
			var principal = oauthToken.getPrincipal();
			
			// GitHub/Kakao OAuth2User attributes
			Object idAttr = principal.getAttribute("id");
			String providerId = idAttr != null ? idAttr.toString() : null;
			String username = principal.getAttribute("login");
			String email = principal.getAttribute("email");
			String avatarUrl = principal.getAttribute("avatar_url");
			
			if (username == null || username.trim().isEmpty()) {
				return;
			}
			
			// Save user info to bluesky-api-user
			var request = new UserInfoApiClient.SaveOAuth2UserRequest(
				provider,
				providerId,
				username,
				email,
				avatarUrl
			);
			
			userInfoApiClient.saveOAuth2User(request);
		} catch (Exception e) {
			// Log error but allow login to proceed
			e.printStackTrace();
		}
	}

	/**
	 * Normalize provider name (github-local → github)
	 */
	private String normalizeProvider(String provider) {
		if (provider == null) {
			return null;
		}
		if (provider.startsWith("github")) {
			return "github";
		}
		if (provider.startsWith("kakao")) {
			return "kakao";
		}
		return provider;
	}
}
