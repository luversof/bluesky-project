package net.luversof.security.oauth2.provider.token;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.user.domain.User;
import net.luversof.user.domain.UserType;
import net.luversof.user.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BattleNetUserAuthenticationConverter implements UserAuthenticationConverter {
	
	@Autowired
	private UserService userService;
	
	private Collection<? extends GrantedAuthority> defaultAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList(StringUtils.arrayToCommaDelimitedString(UserType.BATTLENET.getAuthorities()));
	
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
		if (map.containsKey("id") && map.containsKey("battletag")) {
			String username = String.valueOf(map.get("battletag"));
			UserType userType = UserType.BATTLENET;
			String externalId = String.valueOf(map.get("id"));
			User user = userService.findByExternalIdAndUserType(externalId, userType);
			if (user == null) {
				user = userService.addUser(username, userType, externalId);
			}
			BlueskyUser luversofUser = new BlueskyUser(user);
			return new UsernamePasswordAuthenticationToken(luversofUser, "N/A", getAuthorities(map));
		}
		return null;
	}
	
	private Collection<? extends GrantedAuthority> getAuthorities(Map<String, ?> map) {
		return defaultAuthorities;
	}

}
