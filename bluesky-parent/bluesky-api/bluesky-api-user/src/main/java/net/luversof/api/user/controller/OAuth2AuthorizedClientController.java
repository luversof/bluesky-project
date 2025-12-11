package net.luversof.api.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.web.servlet.util.ServletRequestDataBinderUtil;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping(value = "/api/oAuth2AuthorizedClient", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnBean(JdbcOAuth2AuthorizedClientService.class)
public class OAuth2AuthorizedClientController {

	private JdbcOAuth2AuthorizedClientService oAuth2AuthorizedClientService;

	@Autowired
	public void setOAuth2AuthorizedClientService(JdbcOAuth2AuthorizedClientService oAuth2AuthorizedClientService) {
		this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
	}

	private JsonMapper jsonMapper;

	OAuth2AuthorizedClientController() {
		// Jackson 3 에서는 JsonMapper.builder() 사용
		var builder = JsonMapper.builder();

		// 기존 SecurityJackson2Modules 등록 (Jackson 3에서도 동일)
		for (var module : SecurityJacksonModules.getModules(getClass().getClassLoader())) {
			builder.addModule(module);
		}

		// XML 비활성화: createXmlMapper(false) → JSON 전용 JsonMapper 이므로 기본적으로 XML 없음
		this.jsonMapper = builder.build();
	}

	@GetMapping
	public OAuth2AuthorizedClient loadAuthorizedClient(@RequestParam String clientRegistrationId,
			@RequestParam String principalName) {
		return oAuth2AuthorizedClientService.loadAuthorizedClient(clientRegistrationId, principalName);
	}

	@PostMapping
	public void saveAuthorizedClient() {
		var saveAuthorizedClientParam = ServletRequestDataBinderUtil.getRequestBodyObject(jsonMapper,
				SaveAuthorizedClientParam.class);
		oAuth2AuthorizedClientService.saveAuthorizedClient(saveAuthorizedClientParam.authorizedClient(),
				saveAuthorizedClientParam.principal());
	}

	@DeleteMapping
	public void removeAuthorizedClient(@RequestParam String clientRegistrationId, @RequestParam String principalName) {
		oAuth2AuthorizedClientService.removeAuthorizedClient(clientRegistrationId, principalName);
	}

	private static record SaveAuthorizedClientParam(OAuth2AuthorizedClient authorizedClient,
			OAuth2AuthenticationToken principal) {

	}

}
