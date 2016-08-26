package net.luversof.user.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum UserType {
	LOCAL(new String[]{ "ROLE_USER" }),
	GITHUB(new String[]{ "ROLE_USER", "ROLE_USER_GITHUB" }), 
	FACEBOOK(new String[]{ "ROLE_USER", "ROLE_USER_FACEBOOK" }), 
	BATTLENET(new String[]{ "ROLE_USER", "ROLE_USER_BATTLENET" });
	
	@Getter private String[] authorities;
}
