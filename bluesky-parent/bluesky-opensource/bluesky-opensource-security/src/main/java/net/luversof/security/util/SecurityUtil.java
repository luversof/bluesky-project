package net.luversof.security.util;

import java.util.Optional;

import lombok.Setter;
import net.luversof.security.service.SecurityLoginUserService;
import net.luversof.user.domain.User;

public abstract class SecurityUtil {

	@Setter
	public static SecurityLoginUserService securityLoginUserService;
	
	public static Optional<User> getUser() {
		return securityLoginUserService.getUser();
	}
}
