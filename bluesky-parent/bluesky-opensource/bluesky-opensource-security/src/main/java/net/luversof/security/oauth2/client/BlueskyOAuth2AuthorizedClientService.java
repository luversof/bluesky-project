package net.luversof.security.oauth2.client;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import net.luversof.user.domain.User;
import net.luversof.user.domain.UserType;
import net.luversof.user.service.UserService;

/**
 * db로 완전히 관리하지 못하므로 임시로 inmemory 형태를 사용함
 * @author bluesky
 *
 */
@Service
public class BlueskyOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {
	
	private final Map<String, OAuth2AuthorizedClient> authorizedClients = new ConcurrentHashMap<>();
	private final ClientRegistrationRepository clientRegistrationRepository;
	
	public BlueskyOAuth2AuthorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
		Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
		this.clientRegistrationRepository = clientRegistrationRepository;
	}
	
	@Autowired
	private UserService userService;

	@SuppressWarnings("unchecked")
	@Override
	public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
		Optional<User> user = userService.findByExternalIdAndUserType(principalName, UserType.findByName(clientRegistrationId));
		if (user.isEmpty()) {
			return null;
		}
		
		ClientRegistration registration = this.clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
		if (registration == null) {
			return null;
		}
		return (T) this.authorizedClients.get(this.getIdentifier(registration, principalName));
	}

	@Override
	public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
		Optional<User> user = userService.findByExternalIdAndUserType(principal.getName(), UserType.findByName(authorizedClient.getClientRegistration().getRegistrationId()));
		if (user.isEmpty()) {
			List<String> authorityList = principal.getAuthorities().stream().map(grantedAuthority -> grantedAuthority.getAuthority()).collect(Collectors.toList());
			userService.addUser(principal.getName(), UserType.findByName(authorizedClient.getClientRegistration().getRegistrationId()), principal.getName(), authorityList);
		}
		
		this.authorizedClients.put(this.getIdentifier(authorizedClient.getClientRegistration(), principal.getName()), authorizedClient);
	}

	@Override
	public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
		ClientRegistration registration = this.clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
		if (registration != null) {
			this.authorizedClients.remove(this.getIdentifier(registration, principalName));
		}
	}

	private String getIdentifier(ClientRegistration registration, String principalName) {
		String identifier = "[" + registration.getRegistrationId() + "][" + principalName + "]";
		return Base64.getEncoder().encodeToString(identifier.getBytes());
	}
}
