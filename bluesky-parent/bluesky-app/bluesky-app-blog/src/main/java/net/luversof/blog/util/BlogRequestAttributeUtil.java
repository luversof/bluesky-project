package net.luversof.blog.util;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import lombok.Setter;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.BlogService;
import net.luversof.blog.service.BlogUserService;
import net.luversof.core.util.AbstractRequestAttributeUtil;

/**
 * 로그인한 유저의 정보 반환에 대해 반복 호출을 방지하는 유틸
 * @author bluesky
 *
 */
public class BlogRequestAttributeUtil extends AbstractRequestAttributeUtil {
	
	private static final String USER_ID = "__user_id";
	private static final String USER_BLOG_LIST = "__user_blog_list";
	
	@Setter
	private static BlogService blogService;
	
	@Setter
	private static BlogUserService blogUserService;
	
	public static Optional<String> getUserId() {
		Optional<String> userIdOptional = getRequestAttribute(USER_ID);
		if (userIdOptional != null) {
			return userIdOptional;
		}
		
		userIdOptional = blogUserService.getUserId();
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
		
		blogList = blogService.findByUser(userIdOptional.get());
		setRequestAttribute(USER_BLOG_LIST, blogList);
		return blogList;
	}
	
	
}
