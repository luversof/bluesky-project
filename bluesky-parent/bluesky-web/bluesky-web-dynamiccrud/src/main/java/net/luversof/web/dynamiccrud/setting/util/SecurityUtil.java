package net.luversof.web.dynamiccrud.setting.util;

import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {
	
	
	/**
	 * 현재 인증 주체가 지정한 authority 중 하나라도 가지고 있는지 확인.
	 * (RoleHierarchy에 의존하지 않도록 필요한 role을 명시적으로 나열해 호출한다. 예: ROLE_MASTER, ROLE_ADMIN)
	 */
	public static boolean hasAnyAuthority(String... authorities) {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			return false;
		}
		for (var granted : auth.getAuthorities()) {
			for (String authority : authorities) {
				if (granted.getAuthority().equals(authority)) {
					return true;
				}
			}
		}
		return false;
	}
}
