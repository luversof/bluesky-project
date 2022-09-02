package net.luversof.core.util;

import io.github.luversof.boot.util.ApplicationContextUtil;
import net.luversof.core.service.UserIdService;

public abstract class CoreUtil {
	
	public static String getUserId() {
		var userService = ApplicationContextUtil.getApplicationContext().getBean(UserIdService.class);
		return userService.getUserId();
	}
}
