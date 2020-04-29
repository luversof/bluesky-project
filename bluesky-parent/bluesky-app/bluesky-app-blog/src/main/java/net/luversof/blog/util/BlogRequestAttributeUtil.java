package net.luversof.blog.util;

import java.util.Optional;

import lombok.Setter;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.BlogService;
import net.luversof.core.util.RequestAttributeUtil;
import net.luversof.user.domain.User;
import net.luversof.user.util.UserUtil;

/**
 * 로그인한 유저의 정보 반환에 대해 반복 호출을 방지하는 유틸
 * @author bluesky
 *
 */
public class BlogRequestAttributeUtil extends RequestAttributeUtil {
	
	private static final String USER_BLOG = "__user_blog";
	
	@Setter
	private static BlogService blogService;
	
	public static Optional<Blog> getUserBlog() {
		Optional<Blog> userBlogOptional = getRequestAttribute(USER_BLOG);
		if (userBlogOptional != null) {
			return userBlogOptional;
		}
		Optional<User> loginUser = UserUtil.getLoginUser();
		if (loginUser.isEmpty()) {
			return Optional.empty();
		}
		
		userBlogOptional = blogService.findByUserId();
		setRequestAttribute(USER_BLOG, userBlogOptional);
		return userBlogOptional;
	}
	
	
}
