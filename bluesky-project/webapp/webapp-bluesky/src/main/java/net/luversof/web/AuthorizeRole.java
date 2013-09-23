package net.luversof.web;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access=AccessLevel.PRIVATE)
public class AuthorizeRole {
	public static final String PRE_AUTHORIZE_ROLE = "hasRole('ROLE_USER')";
}
