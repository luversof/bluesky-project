package net.luversof.user.util;

import java.util.Optional;

import io.github.luversof.boot.autoconfigure.devcheck.core.annotation.DevCheckDescription;
import io.github.luversof.boot.autoconfigure.devcheck.core.annotation.DevCheckUtil;
import io.github.luversof.boot.util.RequestAttributeUtil;
import lombok.Setter;
import net.luversof.user.domain.User;
import net.luversof.user.service.LoginUserService;

@DevCheckUtil
public abstract class UserUtil extends RequestAttributeUtil {
	
	@Setter(onMethod_ = @DevCheckDescription(displayable = false))
	private static LoginUserService loginUserService;
	
	private static final String LOGIN_USER = "__loginUser";

	@DevCheckDescription("get loginUser")
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
