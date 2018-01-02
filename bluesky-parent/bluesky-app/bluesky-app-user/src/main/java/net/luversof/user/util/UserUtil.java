package net.luversof.user.util;

import java.util.Optional;

import lombok.Setter;
import net.luversof.core.util.RequestAttributeUtil;
import net.luversof.user.domain.User;
import net.luversof.user.service.LoginUserService;

public abstract class UserUtil extends RequestAttributeUtil {
	
	@Setter
	private static LoginUserService loginUserService;
	
	private static final String LOGIN_USER = "__loginUser";

	public static Optional<User> getLoginUser() {
		Optional<User> userOptional = getRequestAttribute(LOGIN_USER);
		if (userOptional != null) {
			return userOptional;
		}
		
		userOptional = loginUserService.getUser();
		setRequestAttribute(LOGIN_USER, userOptional);
		
		return userOptional;
	}
}
