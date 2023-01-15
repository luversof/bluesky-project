package net.luversof.api.gate.security.support;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import net.luversof.api.gate.security.domain.BlueskyUser;
import net.luversof.api.gate.security.service.SecurityUserService;


public class BlueskyUserHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {
	
	private final SecurityUserService securityLoginUserService;
	
	public BlueskyUserHandlerMethodArgumentResolver(SecurityUserService securityLoginUserService) {
		this.securityLoginUserService = securityLoginUserService;
	}
	
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return BlueskyUser.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		var user = securityLoginUserService.getUser().orElse(null);
		if (user == null) {
			return null;
		}
		return new BlueskyUser(user);
	}

}
