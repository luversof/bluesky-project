package net.luversof.client.user.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Common OAuth2 Login Success Handler for all bluesky-web modules
 * Saves user information to bluesky-api-user after successful OAuth2 login
 */
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private OAuth2AuthorizedClientRepository authorizedClientRepository;

	@Autowired
	public void setAuthorizedClientRepository(OAuth2AuthorizedClientRepository authorizedClientRepository) {
		this.authorizedClientRepository = authorizedClientRepository;
	}

	public OAuth2LoginSuccessHandler() {
		setDefaultTargetUrl("/");
		setAlwaysUseDefaultTargetUrl(false);
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
			String registrationId = oauthToken.getAuthorizedClientRegistrationId();

			authorizedClientRepository.loadAuthorizedClient(
					registrationId,
					authentication,
					request);
		}

		if (request.getSession() != null) {
			String redirectUrl = (String) request.getSession().getAttribute("redirectUrl");
			if (redirectUrl != null) {
				request.getSession().removeAttribute("redirectUrl");
				getRedirectStrategy().sendRedirect(request, response, redirectUrl);
				return;
			}
		}

		super.onAuthenticationSuccess(request, response, authentication);
	}
}
