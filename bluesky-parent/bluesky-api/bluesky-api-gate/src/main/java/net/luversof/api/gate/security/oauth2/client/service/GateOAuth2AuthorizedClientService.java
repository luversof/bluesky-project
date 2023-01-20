package net.luversof.api.gate.security.oauth2.client.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.util.ApplicationContextUtil;
import net.luversof.api.gate.security.oauth2.client.client.OAuth2AuthorizedClientClient;
import net.luversof.api.gate.security.oauth2.client.client.OAuth2AuthorizedClientClient.SaveAuthorizedClientParam;

@Service
public class GateOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
		return (T) getClient().loadAuthorizedClient(clientRegistrationId, principalName);
	}

	@Override
	public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
		getClient().saveAuthorizedClient(new SaveAuthorizedClientParam(authorizedClient, principal));
	}

	@Override
	public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
		getClient().removeAuthorizedClient(clientRegistrationId, principalName);
	}
	
	private OAuth2AuthorizedClientClient getClient() {
		return ApplicationContextUtil.getApplicationContext().getBean(OAuth2AuthorizedClientClient.class);
	}

}
