package net.luversof.security.oauth2.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientId;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import net.luversof.user.domain.UserType;
import net.luversof.user.service.UserService;

/**
 * db로 완전히 관리하지 못하므로 임시로 inmemory 형태를 사용함
 * @author bluesky
 *
 */
@Service
public class BlueskyOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {
	
	private final Map<OAuth2AuthorizedClientId, OAuth2AuthorizedClient> authorizedClients;
	private final ClientRegistrationRepository clientRegistrationRepository;
	
	public BlueskyOAuth2AuthorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
		Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
		this.clientRegistrationRepository = clientRegistrationRepository;
		this.authorizedClients = new ConcurrentHashMap<>();
	}
	
	@Autowired
	private UserService userService;

	@SuppressWarnings("unchecked")
	@Override
	public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
		Assert.hasText(clientRegistrationId, "clientRegistrationId cannot be empty");
		Assert.hasText(principalName, "principalName cannot be empty");
		
		var user = userService.findByExternalIdAndUserType(principalName, UserType.findByName(clientRegistrationId));
		if (user.isEmpty()) {
			return null;
		}
		
		var registration = this.clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
		if (registration == null) {
			return null;
		}
		return (T) this.authorizedClients.get(new OAuth2AuthorizedClientId(clientRegistrationId, principalName));
	}

	@Override
	public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
		Assert.notNull(authorizedClient, "authorizedClient cannot be null");
		Assert.notNull(principal, "principal cannot be null");
		
		var user = userService.findByExternalIdAndUserType(principal.getName(), UserType.findByName(authorizedClient.getClientRegistration().getRegistrationId()));
		if (user.isEmpty()) {
			var authorityList = principal.getAuthorities().stream().map(grantedAuthority -> grantedAuthority.getAuthority()).collect(Collectors.toList());
			
			userService.addUser(getUserName(principal), UserType.findByName(authorizedClient.getClientRegistration().getRegistrationId()), principal.getName(), authorityList);
		}
		
		this.authorizedClients.put(new OAuth2AuthorizedClientId(authorizedClient.getClientRegistration().getRegistrationId(), principal.getName()), authorizedClient);
	}
	
	private String getUserName(Authentication principal) {
		String userName = principal.getName();
		if (!(principal instanceof OAuth2AuthenticationToken)) {
			return userName;
		}
		var oAuth2AuthenticationToken = (OAuth2AuthenticationToken) principal;
		UserType userType = UserType.findByName(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId());
		if (userType == null || userType.getUserNameAttributeKey() == null) {
			return userName;
		}
		return oAuth2AuthenticationToken.getPrincipal().getAttribute(userType.getUserNameAttributeKey());
	}

	@Override
	public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
		Assert.hasText(clientRegistrationId, "clientRegistrationId cannot be empty");
		Assert.hasText(principalName, "principalName cannot be empty");
		ClientRegistration registration = this.clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
		if (registration != null) {
			this.authorizedClients.remove(new OAuth2AuthorizedClientId(clientRegistrationId, principalName));
		}
	}
}
