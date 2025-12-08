package net.luversof.client.user.httpexchange;

import java.util.List;
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
			@RequestParam("provider") String provider,
			@RequestParam("providerId") String providerId);

	@GetExchange("/api/userInfo/search/findByIdIn")
	List<UserInfoResponse> findByIdIn(@RequestParam("ids") List<UUID> ids);

	record SaveOAuth2UserRequest(
		String provider,
		String providerId,
		String username,
		String email,
		String avatarUrl
	) {}

	record UserInfoResponse(
		String id,
		String username,
		String provider,
		String providerId,
		String email,
		String avatarUrl
	) {}
	
}
