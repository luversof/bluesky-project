package net.luversof.web.util;

import javax.servlet.http.HttpServletRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access=AccessLevel.PRIVATE)
public class MenuUtil {
	
	public static String getMenu(HttpServletRequest request) {
		String servletPath = request.getServletPath();
		
		return null;
	}

}
