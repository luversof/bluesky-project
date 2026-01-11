package net.luversof.client.user.httpexchange;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "/api/userInfo", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface UserInfoApiClient {

	@PostExchange("/oauth2")
	UserInfoResponse saveOAuth2User(@RequestBody SaveOAuth2UserRequest request);

	@GetExchange("/search/findByProvider")
	UserInfoResponse findByProviderAndProviderId(
			@RequestParam String provider,
			@RequestParam String providerId);

	@GetExchange("/search/findByIdIn")
	List<UserInfoResponse> findByIdIn(@RequestParam List<UUID> ids);

	@GetExchange("/validate-session")
	UserInfoResponse validateSession(@RequestParam String sessionId);

	@PostExchange("/create-session")
	void createSession(@RequestBody CreateSessionRequest request);

	@PostExchange("/create-new-session")
	String createNewSession();

	@PostExchange("/delete-session")
	void deleteSession(@RequestBody DeleteSessionRequest request);

	record SaveOAuth2UserRequest(
			String provider,
			String providerId,
			String username,
			String email,
			String avatarUrl) implements Serializable {
	}

	record CreateSessionRequest(
			String sessionId,
			Map<String, Object> sessionAttributes) implements Serializable {
	}

	record DeleteSessionRequest(
			String sessionId) implements Serializable {
	}

	record UserInfoResponse(
			String id,
			String username,
			String provider,
			String providerId,
			String email,
			String avatarUrl,
			List<String> authorities,
			Map<String, Object> sessionAttributes) implements Serializable {
	}

}
