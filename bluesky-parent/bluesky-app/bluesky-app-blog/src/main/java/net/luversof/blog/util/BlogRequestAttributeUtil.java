package net.luversof.blog.util;

import java.util.List;

import org.springframework.context.ApplicationContext;

import io.github.luversof.boot.util.RequestAttributeUtil;
import lombok.Setter;
import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.service.BlogArticleService;
import net.luversof.blog.service.BlogService;

/**
 * 로그인한 유저의 정보 반환에 대해 반복 호출을 방지하는 유틸
 * @author bluesky
 *
 */
public class BlogRequestAttributeUtil extends RequestAttributeUtil {
	
	private static final String BLOGLIST_BY_USERID = "__blogList_by_userId_{0}";
	
	@Setter
	private static ApplicationContext applicationContext;
	
	public static BlogService getBlogService() {
		return applicationContext.getBean(BlogService.class);
	}

	public static BlogArticleService getBlogArticleService() {
		return applicationContext.getBean(BlogArticleService.class);
	}
	
	public static List<Blog> getBlogListByUserId(String userId) {
		var attributeName = getAttributeName(BLOGLIST_BY_USERID, userId);
		List<Blog> blogList = getRequestAttribute(attributeName);
		if (blogList != null) {
			return blogList;
		}
		
		blogList = getBlogService().findByUserId(userId);
		setRequestAttribute(BLOGLIST_BY_USERID, blogList);
		return blogList;
	}
	
//	public static Optional<Blog> getUserBlog() {
//		Optional<Blog> userBlogOptional = getRequestAttribute(USER_BLOG);
//		if (userBlogOptional != null) {
//			return userBlogOptional;
//		}
//		
//		var loginUser = UserUtil.getLoginUser();
//		if (loginUser.isEmpty()) {
//			return Optional.empty();
//		}
//		
//		userBlogOptional = getBlogService().findByUserId();
//		setRequestAttribute(USER_BLOG, userBlogOptional);
//		return userBlogOptional;
//	}
	

}
