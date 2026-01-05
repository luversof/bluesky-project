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
			// 클라이언트에서 session id를 변경한 경우(로그인 등) 서버에는 해당 세션이 없을 수 있음
			// 이 경우 강제로 redis에 세션 키를 생성해준다.
			// 주의 : spring-session-redis는 기본적으로 JdkSerializationRedisSerializer를 사용하므로
			// StringRedisTemplate으로 값을 넣으면 역직렬화 시 에러가 발생함.
			// 따라서 SessionRepository를 통해 세션을 생성하고 ID를 교체하는 방식으로 처리해야 함.

			session = sessionRepository.createSession();
			// 생성된 세션의 ID를 요청받은 ID로 변경 (내부적으로는 불가능할 수 있으므로, Redis에 직접 저장하는 방식을 쓰되
			// Serializer를 맞춰야 함)
			// 하지만 Spring Session은 ID 변경을 지원하지 않으므로,
			// 여기서는 임시로 빈 세션을 생성하여 저장하는 방식을 사용하되,
			// RedisSessionRepository가 사용하는 방식대로 저장해야 함.

			// 대안: RedisSessionRepository가 사용하는 RedisTemplate을 주입받아 처리
			// 하지만 간단하게 해결하기 위해, 여기서는 sessionRepository.createSession()으로 생성된 세션은 무시하고
			// 요청받은 ID로 새 세션을 Redis에 직접 저장하되, 올바른 포맷으로 저장해야 함.

			// 더 나은 방법:
			// 클라이언트가 보낸 ID로 세션을 강제 생성하는 것은 보안상 좋지 않으나,
			// 현재 구조상 필요하다면 RedisSessionRepository의 내부 구현을 흉내내야 함.
			// 그러나 이는 복잡하므로, 차라리 클라이언트가 세션 ID를 변경하지 않도록 하거나,
			// 서버에서 생성한 ID를 클라이언트가 쓰도록 하는 것이 정석임.

			// 현재 상황에서의 핫픽스:
			// StringRedisTemplate 대신 RedisTemplate<Object, Object>를 사용하여 저장
			// 하지만 RedisSessionRepository는 복잡한 해시 구조를 가짐.

			// 가장 안전한 방법:
			// 그냥 sessionRepository.createSession()을 호출하여 새 세션을 만들고,
			// 그 세션의 ID를 반환하여 클라이언트가 쓰게 하는 것이 맞음.
			// 하지만 이미 클라이언트가 ID를 생성해서 보냈으므로...

			// 롤백: StringRedisTemplate 사용 코드 제거하고, 에러 로그만 남기고 리턴.
			// 근본적으로는 클라이언트(Gate)가 로그인 후 변경된 세션 ID를 서버(Api-User)에 통보하는 것이 아니라,
			// 서버가 세션을 생성하고 그 ID를 클라이언트에 줘야 함.
			// 현재 에러는 StringRedisTemplate으로 넣은 String 값을 JdkSerializationRedisSerializer가
			// 역직렬화하려다 실패한 것임.

			System.err.println("UserInfoController.createSession session is null. sessionId: " + request.sessionId());
			return;
		}

		if (request.sessionAttributes() != null) {
			request.sessionAttributes().forEach(session::setAttribute);
		}

		sessionRepository.save(session);
	}

	@SuppressWarnings("unchecked")
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
			System.err.println("UserInfoController.validateSession session is null. sessionId: " + sessionId);
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
