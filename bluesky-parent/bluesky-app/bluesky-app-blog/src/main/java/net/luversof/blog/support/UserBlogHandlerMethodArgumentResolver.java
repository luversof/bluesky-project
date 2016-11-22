package net.luversof.blog.support;

import java.util.Arrays;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import net.luversof.blog.annotation.UserBlog;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.util.BlogRequestAttributeUtil;

/**
 * 유저의 Blog 정보를 반환하는 Resolver
 * 유저가 여러개의 블로그를 가지고 있으면 어쩌지?
 * @author bluesky
 *
 */
public class UserBlogHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Blog.class.isAssignableFrom(parameter.getParameterType()) && Arrays.asList(parameter.getParameterAnnotations()).stream().anyMatch(annotation -> annotation.annotationType().isAssignableFrom(UserBlog.class));
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		List<Blog> userBlogList = BlogRequestAttributeUtil.getUserBlogList();
		return userBlogList.isEmpty() ? new Blog() : userBlogList.get(0);
	}

}
