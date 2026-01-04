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
		Session session = sessionRepository.findById(request.sessionId());
		if (session == null) {
			System.err.println("UserInfoController.createSession session is null. sessionId: " + request.sessionId());
			return;
		}

		if (request.sessionAttributes() != null) {
			request.sessionAttributes().forEach(session::setAttribute);
		}

		sessionRepository.save(session);
	}

	@PostMapping("/create-new-session")
	public String createNewSession() {
		Session session = sessionRepository.createSession();
		sessionRepository.save(session);
		return session.getId();
	}

	@PostMapping("/delete-session")
	public void deleteSession(@RequestBody DeleteSessionRequest request) {
		sessionRepository.deleteById(request.sessionId());
	}

	public record CreateSessionRequest(
			String sessionId,
			Map<String, Object> sessionAttributes) {
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
			List<String> authorities,
			Map<String, Object> sessionAttributes) {
	}

	@GetMapping("/validate-session")
	public UserInfoResponse validateSession(@RequestParam("sessionId") String sessionId) {
		Session session = sessionRepository.findById(sessionId);
		if (session == null) {
			return null;
		}

		Map<String, Object> sessionAttributes = new HashMap<>();
		session.getAttributeNames().forEach(name -> sessionAttributes.put(name, session.getAttribute(name)));

		return new UserInfoResponse(null, null, null, null, null, null, null, sessionAttributes);
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
