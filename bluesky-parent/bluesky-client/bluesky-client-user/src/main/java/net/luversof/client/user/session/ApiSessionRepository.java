package net.luversof.client.user.session;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.MapSession;
import org.springframework.session.SessionRepository;

import net.luversof.client.user.httpexchange.UserInfoApiClient;
import net.luversof.client.user.httpexchange.UserInfoApiClient.CreateSessionRequest;
import net.luversof.client.user.httpexchange.UserInfoApiClient.DeleteSessionRequest;
import net.luversof.client.user.httpexchange.UserInfoApiClient.UserInfoResponse;

public class ApiSessionRepository implements SessionRepository<MapSession> {

	private final UserInfoApiClient userInfoApiClient;

	public ApiSessionRepository(UserInfoApiClient userInfoApiClient) {
		this.userInfoApiClient = userInfoApiClient;
	}

	@Override
	public MapSession createSession() {
		String sessionId = userInfoApiClient.createNewSession();
		return new MapSession(sessionId);
	}

	@Override
	public void save(MapSession session) {
		SecurityContext securityContext = session
				.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
		if (securityContext == null) {
			return;
		}

		Authentication authentication = securityContext.getAuthentication();
		if (authentication == null) {
			return;
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof OAuth2User oauth2User) {
			String id = oauth2User.getAttribute("id");
			String username = oauth2User.getAttribute("username");
			String provider = oauth2User.getAttribute("provider");
			String providerId = oauth2User.getAttribute("providerId");
			String email = oauth2User.getAttribute("email");
			String avatarUrl = oauth2User.getAttribute("avatarUrl");
			List<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
					.toList();

			UserInfoResponse userInfo = new UserInfoResponse(id, username, provider, providerId, email, avatarUrl,
					authorities);
			userInfoApiClient.createSession(new CreateSessionRequest(session.getId(), userInfo));
		}
	}

	@Override
	public MapSession findById(String id) {
		try {
			var userInfo = userInfoApiClient.validateSession(id);
			if (userInfo == null) {
				return null;
			}

			MapSession session = new MapSession(id);

			var authorities = userInfo.authorities() != null
					? AuthorityUtils.createAuthorityList(userInfo.authorities().toArray(new String[0]))
					: AuthorityUtils.NO_AUTHORITIES;

			Map<String, Object> attributes = new HashMap<>();
			attributes.put("id", userInfo.id());
			attributes.put("username", userInfo.username());
			attributes.put("provider", userInfo.provider());
			attributes.put("providerId", userInfo.providerId());
			attributes.put("email", userInfo.email());
			attributes.put("avatarUrl", userInfo.avatarUrl());

			OAuth2User principal = new DefaultOAuth2User(authorities, attributes, "username");

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					principal, "N/A", authorities);

			SecurityContext securityContext = new SecurityContextImpl(authentication);

			session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
			session.setLastAccessedTime(Instant.now());
			session.setMaxInactiveInterval(Duration.ofMinutes(30));

			return session;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void deleteById(String id) {
		userInfoApiClient.deleteSession(new DeleteSessionRequest(id));
	}

}
