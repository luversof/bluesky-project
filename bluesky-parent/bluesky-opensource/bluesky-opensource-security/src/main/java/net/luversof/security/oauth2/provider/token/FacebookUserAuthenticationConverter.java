package net.luversof.security.oauth2.provider.token;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.security.core.userdetails.UserType;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.util.StringUtils;

public class FacebookUserAuthenticationConverter implements UserAuthenticationConverter {
	
	private Collection<? extends GrantedAuthority> defaultAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList(StringUtils.arrayToCommaDelimitedString(new String[]{ "ROLE_USER", "ROLE_FACEBOOKUSER" }));
	
	public void setDefaultAuthorities(String[] defaultAuthorities) {
		this.defaultAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList(StringUtils.arrayToCommaDelimitedString(defaultAuthorities));
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

	@Override
	public Authentication extractAuthentication(Map<String, ?> map) {
		if (map.containsKey("id") && map.containsKey("name")) {
			BlueskyUser luversofUser = new BlueskyUser(Long.valueOf((String) map.get("id")), (String) map.get("name"), "N/A", getAuthorities(map), true, true, true, true, UserType.FACEBOOK);
			return new UsernamePasswordAuthenticationToken(luversofUser, "N/A", getAuthorities(map));
		}
		return null;
	}
	
	private Collection<? extends GrantedAuthority> getAuthorities(Map<String, ?> map) {
		return defaultAuthorities;
	}

}
