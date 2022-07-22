package net.luversof.security.util;

import java.util.Optional;

import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.security.service.SecurityLoginUserService;
import net.luversof.user.domain.User;

@UtilityClass
public class SecurityUtil {

	@Setter
	public static SecurityLoginUserService securityLoginUserService;
	
	public static Optional<User> getUser() {
		return securityLoginUserService.getUser();
	}
	
	public static BlueskyUser getBlueskyUser() {
		User user = getUser().orElse(null);
		if (user == null) {
			return null;
		}
		return new BlueskyUser(user);
	}
}
