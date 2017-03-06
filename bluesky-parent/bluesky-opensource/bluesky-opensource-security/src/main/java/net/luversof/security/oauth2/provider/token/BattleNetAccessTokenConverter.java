package net.luversof.security.oauth2.provider.token;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.stereotype.Component;

@Component
public class BattleNetAccessTokenConverter implements AccessTokenConverter {

	@Autowired
	private BattleNetUserAuthenticationConverter battleNetUserAuthenticationConverter;

	@Override
	public Map<String, ?> convertAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
		Map<String, Object> response = new HashMap<String, Object>();
		OAuth2Request clientToken = authentication.getOAuth2Request();

		if (!authentication.isClientOnly()) {
			response.putAll(battleNetUserAuthenticationConverter.convertUserAuthentication(authentication.getUserAuthentication()));
		}

		if (token.getScope() != null) {
			response.put(SCOPE, token.getScope());
		}
		if (token.getAdditionalInformation().containsKey(JTI)) {
			response.put(JTI, token.getAdditionalInformation().get(JTI));
		}

		if (token.getExpiration() != null) {
			response.put(EXP, token.getExpiration().getTime() / 1000);
		}

		response.putAll(token.getAdditionalInformation());

		response.put(CLIENT_ID, clientToken.getClientId());
		if (clientToken.getResourceIds() != null && !clientToken.getResourceIds().isEmpty()) {
			response.put(AUD, clientToken.getResourceIds());
		}
		return response;
	}

	@Override
	public OAuth2AccessToken extractAccessToken(String value, Map<String, ?> map) {
		DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(value);
		Map<String, Object> info = new HashMap<String, Object>(map);
		info.remove(EXP);
		info.remove(AUD);
		info.remove(CLIENT_ID);
		info.remove(SCOPE);
		if (map.containsKey(EXP)) {
			token.setExpiration(new Date((Long) map.get(EXP) * 1000L));
		}
		if (map.containsKey(JTI)) {
			info.put(JTI, map.get(JTI));
		}
		@SuppressWarnings("unchecked")
		Collection<String> scope = (Collection<String>) map.get(SCOPE);
		if (scope != null) {
			token.setScope(new HashSet<String>(scope));
		}
		token.setAdditionalInformation(info);
		return token;
	}

	@Override
	public OAuth2Authentication extractAuthentication(Map<String, ?> map) {

		@SuppressWarnings("unchecked")
		Set<String> scope = new LinkedHashSet<String>(map.containsKey("client_id") && map.containsKey("scopes") ? (Collection<String>) map.get("scopes") : Collections.<String> emptySet());
		String clientId = String.valueOf(map.get("client_id"));

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(CLIENT_ID, clientId);

		@SuppressWarnings("unchecked")
		Set<String> resourceIds = new LinkedHashSet<String>(map.containsKey(AUD) ? (Collection<String>) map.get(AUD) : Collections.<String> emptySet());
		OAuth2Request request = new OAuth2Request(parameters, clientId, null, true, scope, resourceIds, null, null, null);
		
		Authentication user = battleNetUserAuthenticationConverter.extractAuthentication(map);
		return new OAuth2Authentication(request, user);
	}

}
