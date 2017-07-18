package net.luversof.web.util;

import java.lang.reflect.Method;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class DevCheckUtil {
	
	private static final DefaultParameterNameDiscoverer DISCOVERER = new DefaultParameterNameDiscoverer();

	public static String getUrlWithParameter(String url, Method method) {
		String[] parameterNames = DISCOVERER.getParameterNames(method);
		if (parameterNames.length == 0) {
			return url;
		}
		StringBuilder stringBuilder = new StringBuilder(url);
		for (int i = 0 ; i < parameterNames.length ; i++) {
			if (method.getParameters()[i].isAnnotationPresent(PathVariable.class)) {
				continue;
			}
			stringBuilder.append(i == 0 ? "?" : "&").append(parameterNames[i]).append("={").append(parameterNames[i]).append("}");
		}
		
		return stringBuilder.toString();
	}
}
