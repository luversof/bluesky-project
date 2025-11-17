package net.luversof.client.user.openfeign;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * bluesky-api-user service Feign Client
 * Common client for all bluesky-web-* modules
 */
@FeignClient(name = "bluesky-api-user", contextId = "api-user", url = "${client.user.feign-client.url:}")
public interface UserApiClient {

	@PostMapping(path = "/api/userInfo/oauth2", consumes = MediaType.APPLICATION_JSON_VALUE)
	UserInfoResponse saveOAuth2User(@RequestBody SaveOAuth2UserRequest request);

	@GetMapping(path = "/api/userInfo/search/findByProvider")
	UserInfoResponse findUserByProvider(
			@RequestParam("provider") String provider,
			@RequestParam("providerId") String providerId);

	@GetMapping(path = "/api/userInfo/search/findByIdIn")
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
