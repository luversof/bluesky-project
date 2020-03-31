package net.luversof.blog.util;

import java.util.Optional;
import java.util.UUID;

import lombok.Setter;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.BlogService;
import net.luversof.core.util.RequestAttributeUtil;
import net.luversof.user.domain.User;
import net.luversof.user.service.LoginUserService;

/**
 * 로그인한 유저의 정보 반환에 대해 반복 호출을 방지하는 유틸
 * @author bluesky
 *
 */
public class BlogRequestAttributeUtil extends RequestAttributeUtil {
	
	private static final String USER_BLOG = "__user_blog";
	
	@Setter
	private static BlogService blogService;
	
	@Setter
	private static LoginUserService loginUserService;
	
	public static UUID getUserId() {
		Optional<User> userOptional = loginUserService.getUser();
		return userOptional.isPresent() ? userOptional.get().getId() : null;
	}
	
	public static Blog getUserBlog() {
		Optional<Blog> userBlogOptional = getRequestAttribute(USER_BLOG);
		if (userBlogOptional != null) {
			return userBlogOptional.orElse(null);
		}
		UUID userId = getUserId();
		if (userId == null) {
			return null;
		}
		
		userBlogOptional = blogService.findByUserId();
		setRequestAttribute(USER_BLOG, userBlogOptional);
		return userBlogOptional.orElse(null);
	}
	
	
}
