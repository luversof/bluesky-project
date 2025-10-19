package net.luversof.api.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.luversof.boot.web.servlet.util.ServletRequestDataBinderUtil;
import lombok.Setter;

@RestController
@RequestMapping(value = "/api/oAuth2AuthorizedClient", produces = MediaType.APPLICATION_JSON_VALUE)
public class OAuth2AuthorizedClientController {

	@Setter(onMethod_ = @Autowired)
	private JdbcOAuth2AuthorizedClientService oAuth2AuthorizedClientService;
	
	private ObjectMapper objectMapper;
	
	OAuth2AuthorizedClientController(Jackson2ObjectMapperBuilder builder) {
		this.objectMapper = builder.createXmlMapper(false).build();
		this.objectMapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
	}
	
	@GetMapping
	public OAuth2AuthorizedClient loadAuthorizedClient(@RequestParam String clientRegistrationId, @RequestParam String principalName) {
		return oAuth2AuthorizedClientService.loadAuthorizedClient(clientRegistrationId, principalName);
	}
	
	@PostMapping
	public void saveAuthorizedClient() {
		var saveAuthorizedClientParam = ServletRequestDataBinderUtil.getRequestBodyObject(objectMapper, SaveAuthorizedClientParam.class);
		oAuth2AuthorizedClientService.saveAuthorizedClient(saveAuthorizedClientParam.authorizedClient(), saveAuthorizedClientParam.principal());
	}
	
	@DeleteMapping
	public void removeAuthorizedClient(@RequestParam String clientRegistrationId, @RequestParam String principalName) {
		oAuth2AuthorizedClientService.removeAuthorizedClient(clientRegistrationId, principalName);
	}

	
	private static record SaveAuthorizedClientParam(OAuth2AuthorizedClient authorizedClient, OAuth2AuthenticationToken principal) {
		
	}
	
}
