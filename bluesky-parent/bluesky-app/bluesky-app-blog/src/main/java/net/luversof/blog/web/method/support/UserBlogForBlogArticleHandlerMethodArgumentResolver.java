package net.luversof.blog.web.method.support;

import java.util.Arrays;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import net.luversof.blog.annotation.UserBlog;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.domain.BlogArticle;
import net.luversof.blog.exception.BlogErrorCode;
import net.luversof.blog.util.BlogRequestAttributeUtil;
import net.luversof.core.exception.BlueskyException;
import net.luversof.core.web.method.support.BlueskyHandlerMethodArgumentResolver;

/**
 * 유저의 Blog 정보를 반환하는 Resolver
 * 유저가 여러개의 블로그를 가지고 있으면 어쩌지?
 * @author bluesky
 *
 */
public class UserBlogForBlogArticleHandlerMethodArgumentResolver extends BlueskyHandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return BlogArticle.class.isAssignableFrom(parameter.getParameterType()) && Arrays.asList(parameter.getParameterAnnotations()).stream().anyMatch(annotation -> annotation.annotationType().isAssignableFrom(UserBlog.class));
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		List<Blog> userBlogList = BlogRequestAttributeUtil.getUserBlogList();
		if (userBlogList.isEmpty()) {
			throw new BlueskyException(BlogErrorCode.NOT_EXIST_BLOG);
		}
		
		BlogArticle requestBlogArticle = (BlogArticle) super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
		
		UserBlog userBlog = (UserBlog) Arrays.asList(parameter.getParameterAnnotations()).stream().filter(annotation -> annotation.annotationType().isAssignableFrom(UserBlog.class)).findAny().get();
		if (userBlog.checkBlog()) {
			if (requestBlogArticle.getBlog() == null || requestBlogArticle.getBlog().getId() == 0) {
				throw new BlueskyException(BlogErrorCode.NOT_EXIST_PARAMETER_BLOG_ID);
			}
			Blog targetBlog = userBlogList.stream().filter(blog -> blog.getId() == requestBlogArticle.getBlog().getId()).findAny().orElseThrow(() -> new BlueskyException(BlogErrorCode.INVALID_PARAMETER_BLOG_ID));	// 유저의 블로그가 아닌 접근인 경우 에러
			requestBlogArticle.setBlog(targetBlog);
		} else {
			requestBlogArticle.setBlog(userBlogList.get(0));
		}
		
		return requestBlogArticle;
	}
}
