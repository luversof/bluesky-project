package net.luversof.blog.util;

import java.util.Optional;
import java.util.UUID;

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
	private static final String USER_BLOG = "__user_blog";
	
	@Setter
	private static BlogService blogService;
	
	@Setter
	private static BlogUserService blogUserService;
	
	public static UUID getUserId() {
		Optional<UUID> userIdOptional = getRequestAttribute(USER_ID);
		if (userIdOptional != null) {
			return userIdOptional.orElse(null);
		}
		
		userIdOptional = blogUserService.getUserId();
		setRequestAttribute(USER_ID, userIdOptional);
		return userIdOptional.orElse(null);
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
		
		userBlogOptional = blogService.findByUserId(userId);
		setRequestAttribute(USER_BLOG, userBlogOptional);
		return userBlogOptional.orElse(null);
	}
	
	
}
