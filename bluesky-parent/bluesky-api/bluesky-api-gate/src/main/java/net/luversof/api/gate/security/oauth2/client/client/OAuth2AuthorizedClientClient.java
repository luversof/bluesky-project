package net.luversof.api.gate.security.oauth2.client.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "bluesky-api-user", contextId = "api-user-oauth2AuthorizedClient", path = "/api/user/oAuth2AuthorizedClient", url = "${gate.feign-client.url.user:}")
public interface OAuth2AuthorizedClientClient {

	@GetMapping
	OAuth2AuthorizedClient loadAuthorizedClient(@RequestParam String clientRegistrationId, @RequestParam String principalName);
	
	@PostMapping
	void saveAuthorizedClient(@RequestBody SaveAuthorizedClientParam saveAuthorizedClientParam);
	
	@DeleteMapping
	void removeAuthorizedClient(@RequestParam String clientRegistrationId, @RequestParam String principalName);
	
	public static record SaveAuthorizedClientParam(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
		
	}
}
