package net.luversof.blog.support;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.ModelFactory;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import net.luversof.blog.annotation.UserBlog;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.exception.BlogErrorCode;
import net.luversof.blog.util.BlogRequestAttributeUtil;
import net.luversof.core.exception.BlueskyException;

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
		String name = ModelFactory.getNameForParameter(parameter);
		Object attribute = (mavContainer.containsAttribute(name) ? mavContainer.getModel().get(name) : BeanUtils.instantiateClass(parameter.getParameterType()));
		
		UserBlog userBlog = (UserBlog) Arrays.asList(parameter.getParameterAnnotations()).stream().filter(annotation -> annotation.annotationType().isAssignableFrom(UserBlog.class)).findAny().get();
		if (userBlog.checkBlog()) {
			WebRequestDataBinder binder = new WebRequestDataBinder(attribute);
			binder.bind(webRequest);
			Blog targetBlog = (Blog) binder.getTarget();
			if (targetBlog.getId() == 0) {
				throw new BlueskyException(BlogErrorCode.NOT_EXIST_PARAMETER_BLOG_ID);
			}
			return userBlogList.stream().filter(blog -> blog.getId() == targetBlog.getId()).findAny().orElseThrow(() -> new BlueskyException(BlogErrorCode.INVALID_PARAMETER_BLOG_ID));	// 유저의 블로그가 아닌 접근인 경우 에러
		}
		
		return userBlogList.isEmpty() ? new Blog() : userBlogList.get(0);
	}
}
