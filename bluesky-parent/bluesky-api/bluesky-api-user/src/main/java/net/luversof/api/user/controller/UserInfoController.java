package net.luversof.api.user.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.user.domain.UserInfo;
import net.luversof.api.user.service.UserInfoService;

@RestController
@RequestMapping(value = "/api/userInfo", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserInfoController {

	private UserInfoService userInfoService;

	@Autowired
	@SuppressWarnings("rawtypes")
	private SessionRepository sessionRepository;

	@Autowired
	public void setUserInfoService(UserInfoService userInfoService) {
		this.userInfoService = userInfoService;
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/create-session")
	public void createSession(@RequestBody CreateSessionRequest request) {
		// We need to save the session with the ID provided by the client.
		// Since we can't easily force the ID on a new RedisSession,
		// we assume the client (ApiSessionRepository) has already coordinated or we
		// accept that we might need a different approach if strict ID matching is
		// required.
		// However, for now, let's try to find the session. If it doesn't exist, we
		// might be in trouble if we can't set the ID.

		// Actually, Spring Session Redis allows creating a session, but the ID is
		// generated.
		// If we want to support "saving" a session from an external source, we might
		// need to use RedisTemplate directly to set the key.
		// But that bypasses the repository logic (expiration, indexes).

		// Let's assume for a moment that we can just create a new session and we don't
		// care if the ID changes?
		// No, the client has the cookie with the ID.

		// Correct approach for "Remote Session Store":
		// The client asks the server to create a session *first*.
		// But `ApiSessionRepository.createSession()` is synchronous and returns a
		// MapSession.
		// We can call the API there.

		// Here, we implement the "save" part.
		// If the session exists, we update it.
		// If it doesn't, we create it.
		// But we must use the `request.sessionId`.

		// Since we can't force ID in `sessionRepository.createSession()`,
		// we will use a workaround: We will use the `sessionRepository` to save, but we
		// need a way to instantiate a Session with a specific ID.
		// RedisSessionRepository.RedisSession is not public or easy to instantiate.

		// Ideally, `bluesky-web-user` should ask `bluesky-api-user` to create the
		// session ID *before* setting the cookie.
		// Let's modify `ApiSessionRepository.createSession` to call an API `POST
		// /create-session-id`?
		// Or `POST /create-session` returns the new ID.

		// But `ApiSessionRepository` implements `SessionRepository<MapSession>`.
		// `MapSession` constructor takes an ID.

		// Let's implement `createSession` in `UserInfoController` to return a new
		// Session ID.
		// And `saveSession` to update the data.

		// But for now, to satisfy the interface, let's implement `createSession` (save)
		// using the provided ID if possible.
		// If we can't, we might need to use `RedisTemplate`.

		// Let's try to use the sessionRepository to find it.
		Session session = sessionRepository.findById(request.sessionId());
		if (session == null) {
			// If it doesn't exist, we can't create it with the specific ID using standard
			// Repository.
			// We will rely on the fact that `ApiSessionRepository.createSession` should
			// have called the API to get a valid ID first.
			// So let's add `createSession` endpoint that returns a new ID.
			return;
		}

		// Populate session
		var userInfo = request.userInfo();
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

		sessionRepository.save(session);
	}

	@PostMapping("/create-new-session")
	public String createNewSession() {
		Session session = sessionRepository.createSession();
		return session.getId();
	}

	@PostMapping("/delete-session")
	public void deleteSession(@RequestBody DeleteSessionRequest request) {
		sessionRepository.deleteById(request.sessionId());
	}

	public record CreateSessionRequest(
			String sessionId,
			UserInfoResponse userInfo) {
	}

	public record DeleteSessionRequest(
			String sessionId) {
	}

	public record UserInfoResponse(
			String id,
			String username,
			String provider,
			String providerId,
			String email,
			String avatarUrl,
			List<String> authorities) {
	}

	@GetMapping("/validate-session")
	public UserInfoResponse validateSession(@RequestParam("sessionId") String sessionId) {
		Session session = sessionRepository.findById(sessionId);
		if (session == null) {
			return null;
		}

		SecurityContext securityContext = session
				.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
		if (securityContext == null || securityContext.getAuthentication() == null) {
			return null;
		}

		Authentication auth = securityContext.getAuthentication();

		String username = auth.getName();
		List<String> authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

		Optional<UserInfo> userInfoOpt = userInfoService.findByUsername(username);
		if (userInfoOpt.isEmpty()) {
			return new UserInfoResponse(null, username, null, null, null, null, authorities);
		}

		UserInfo userInfo = userInfoOpt.get();
		return new UserInfoResponse(
				userInfo.getId().toString(),
				userInfo.getUsername(),
				userInfo.getProvider(),
				userInfo.getProviderId(),
				userInfo.getEmail(),
				userInfo.getAvatarUrl(),
				authorities);
	}

	@GetMapping("/{id}")
	public Optional<UserInfo> findById(@PathVariable UUID id) {
		return userInfoService.findById(id);
	}

	@GetMapping("/search/findByIdIn")
	public List<UserInfo> findByIdIn(@RequestParam("ids") List<UUID> ids) {
		return userInfoService.findByIdIn(ids);
	}

	@GetMapping("/search/findByUsername/{userName}")
	public Optional<UserInfo> findByUsername(@PathVariable String userName) {
		return userInfoService.findByUsername(userName);
	}

	@GetMapping("/search/findByProvider")
	public Optional<UserInfo> findByProviderAndProviderId(
			@RequestParam("provider") String provider,
			@RequestParam("providerId") String providerId) {
		return userInfoService.findByProviderAndProviderId(provider, providerId);
	}

	@PostMapping("/oauth2")
	public UserInfo saveOAuth2User(@RequestBody OAuth2UserRequest request) {
		return userInfoService.saveOAuth2User(
				request.provider(),
				request.providerId(),
				request.username(),
				request.email(),
				request.avatarUrl());
	}

	record OAuth2UserRequest(
			String provider,
			String providerId,
			String username,
			String email,
			String avatarUrl) {
	}

}
