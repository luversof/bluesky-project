package net.luversof.api.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/user/oAuth2AuthorizedClient", produces = MediaType.APPLICATION_JSON_VALUE)
public class OAuth2AuthorizedClientController {

	@Autowired
	private JdbcOAuth2AuthorizedClientService oAuth2AuthorizedClientService;
	
	@GetMapping
	public OAuth2AuthorizedClient loadAuthorizedClient(@RequestParam String clientRegistrationId, @RequestParam String principalName) {
		return oAuth2AuthorizedClientService.loadAuthorizedClient(clientRegistrationId, principalName);
	}
	
	@PostMapping
	public void saveAuthorizedClient(@RequestBody SaveAuthorizedClientParam saveAuthorizedClientParam) {
		oAuth2AuthorizedClientService.saveAuthorizedClient(saveAuthorizedClientParam.authorizedClient(), saveAuthorizedClientParam.principal());
	}
	
	@DeleteMapping
	public void removeAuthorizedClient(@RequestParam String clientRegistrationId, @RequestParam String principalName) {
		oAuth2AuthorizedClientService.removeAuthorizedClient(clientRegistrationId, principalName);
	}

	
	private static record SaveAuthorizedClientParam(OAuth2AuthorizedClient authorizedClient, OAuth2AuthenticationToken principal) {
		
	}
	
}
