package net.luversof.blog.util;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.BlogService;
import net.luversof.blog.service.BlogUserService;

/**
 * 로그인한 유저의 정보 반환에 대해 반복 호출을 방지하는 유틸
 * @author bluesky
 *
 */
public class BlogRequestAttributeUtil {
	
	private static final String USER_ID = "__user_id";
	private static final String USER_BLOG_LIST = "__user_blog_list";
	
	private static BlogService BLOG_SERVICE;
	
	private static BlogUserService BLOG_USER_SERVICE;
	
	public static void setBlogService(BlogService blogService, BlogUserService blogUserService) {
		BLOG_SERVICE = blogService;
		BLOG_USER_SERVICE = blogUserService;
	}

	@SuppressWarnings("unchecked")
	private static <T> T getRequestAttribute(String name) {
		return (T) RequestContextHolder.currentRequestAttributes().getAttribute(name, RequestAttributes.SCOPE_REQUEST);
	}
	
	private static void setRequestAttribute(String name, Object value) {
		RequestContextHolder.currentRequestAttributes().setAttribute(name, value, RequestAttributes.SCOPE_REQUEST);
	}
	
	public static Optional<String> getUserId() {
		Optional<String> userIdOptional = getRequestAttribute(USER_ID);
		if (userIdOptional != null) {
			return userIdOptional;
		}
		
		userIdOptional = BLOG_USER_SERVICE.getUserId();
		setRequestAttribute(USER_ID, userIdOptional);
		return userIdOptional;
	}
	
	public static List<Blog> getUserBlogList() {
		List<Blog> blogList = getRequestAttribute(USER_BLOG_LIST);
		if (blogList != null) {
			return blogList;
		}
		Optional<String> userIdOptional = getUserId();
		if (!userIdOptional.isPresent()) {
			return Collections.emptyList();
		}
		
		blogList = BLOG_SERVICE.findByUser(userIdOptional.get());
		setRequestAttribute(USER_BLOG_LIST, blogList);
		return blogList;
	}
	
	
}
