package net.luversof.blog.util;

import java.util.Optional;

import io.github.luversof.boot.util.RequestAttributeUtil;
import lombok.Setter;
import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.service.BlogService;
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
		
		var loginUser = UserUtil.getLoginUser();
		if (loginUser.isEmpty()) {
			return Optional.empty();
		}
		
		userBlogOptional = blogService.findByUserId();
		setRequestAttribute(USER_BLOG, userBlogOptional);
		return userBlogOptional;
	}
	
	
}
