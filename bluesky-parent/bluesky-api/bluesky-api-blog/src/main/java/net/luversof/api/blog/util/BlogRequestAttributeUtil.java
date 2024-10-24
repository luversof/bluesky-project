package net.luversof.api.blog.util;

import java.util.List;

import org.springframework.context.ApplicationContext;

import io.github.luversof.boot.web.util.RequestAttributeUtil;
import lombok.Setter;
import net.luversof.api.blog.domain.mariadb.Blog;
import net.luversof.api.blog.service.BlogArticleService;
import net.luversof.api.blog.service.BlogService;

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
	
}
