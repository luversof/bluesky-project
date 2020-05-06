package net.luversof.user.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum UserType {
	LOCAL(new String[]{ "ROLE_USER" }, null),
	GITHUB(new String[]{ "ROLE_USER", "ROLE_USER_GITHUB" }, "name"), 
	FACEBOOK(new String[]{ "ROLE_USER", "ROLE_USER_FACEBOOK" }, "name"),
	GOOGLE(new String[]{ "ROLE_USER", "ROLE_USER_GOOGLE" }, "name"),
	BATTLENET(new String[]{ "ROLE_USER", "ROLE_USER_BATTLENET" }, null);
	
	@Getter private String[] authorities;
	@Getter private String userNameAttributeKey;
	
	public static UserType findByName(String name) {
		for (UserType userType : UserType.values()) {
			if (userType.name().equalsIgnoreCase(name)) {
				return userType;
			}
		}
		return null;
	}
}
