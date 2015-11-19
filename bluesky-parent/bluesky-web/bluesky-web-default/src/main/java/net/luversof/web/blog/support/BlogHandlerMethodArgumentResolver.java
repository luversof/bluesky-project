package net.luversof.web.blog.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.BlogService;
import net.luversof.core.exception.BlueskyException;
import net.luversof.security.core.userdetails.BlueskyUser;

@Component
public class BlogHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {
	
	@Autowired
	private BlogService blogService;
	
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Blog.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof BlueskyUser)) {
			throw new PreAuthenticatedCredentialsNotFoundException("BlogHandlerMethodArgumentResolver");
		}
		BlueskyUser user = (BlueskyUser) authentication.getPrincipal();
		
		Blog blog = blogService.findByUser(user.getId(), user.getUserType().name());
		if (blog == null) {
			throw new BlueskyException("blog.notExist");
		}
		return blog;
	}

}
