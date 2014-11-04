package net.luversof.security.oauth2.provider.token;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.util.StringUtils;

public class GithubUserAuthenticationConverter implements UserAuthenticationConverter {
	
	private Collection<? extends GrantedAuthority> defaultAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList(StringUtils.arrayToCommaDelimitedString(new String[]{"user"}));
	
	public void setDefaultAuthorities(String[] defaultAuthorities) {
		this.defaultAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList(StringUtils
				.arrayToCommaDelimitedString(defaultAuthorities));
	}

	@Override
	public Map<String, ?> convertUserAuthentication(Authentication userAuthentication) {
		Map<String, Object> response = new LinkedHashMap<String, Object>();
		response.put(USERNAME, userAuthentication.getName());
		if (userAuthentication.getAuthorities() != null && !userAuthentication.getAuthorities().isEmpty()) {
			response.put(AUTHORITIES, AuthorityUtils.authorityListToSet(userAuthentication.getAuthorities()));
		}
		return response;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Authentication extractAuthentication(Map<String, ?> map) {
		if (map.containsKey("user") && ((Map<String, Object>) map.get("user")).containsKey("login")) {
			return new UsernamePasswordAuthenticationToken(((Map<String, Object>) map.get("user")).get("login"), "N/A", getAuthorities(map));
		}
		return null;
	}

	private Collection<? extends GrantedAuthority> getAuthorities(Map<String, ?> map) {
		return defaultAuthorities;
	}
}
