package net.luversof.security.support;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.security.util.SecurityUtil;
import net.luversof.user.domain.User;

public class BlueskyUserHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {
	
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return BlueskyUser.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		User user = SecurityUtil.getUser().orElse(null);
		if (user == null) {
			return null;
		}
		return new BlueskyUser(user);
	}

}
