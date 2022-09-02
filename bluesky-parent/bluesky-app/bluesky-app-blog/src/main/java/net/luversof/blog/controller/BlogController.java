package net.luversof.blog.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import io.github.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.service.BlogService;
import net.luversof.core.util.CoreUtil;

@Controller("blogControllerForGraphQL")
public class BlogController {
	
	@Autowired
	private BlogService blogService;
	
	@BlueskyPreAuthorize
	@QueryMapping
	public List<Blog> userBlogList() {
		return blogService.findByUserId(CoreUtil.getUserId());
	}

	@BlueskyPreAuthorize
	@MutationMapping
	public Blog createBlog() {
		String userId = CoreUtil.getUserId();
		List<Blog> userBlogList = blogService.findByUserId(userId);
		if (userBlogList != null && !userBlogList.isEmpty()) {
			throw new BlueskyException(BlogErrorCode.ALREADY_EXIST_USER_BLOG);
		}
		
		var blog = new Blog();
		blog.setUserId(userId);
		return blogService.createBlog(blog);
	}

	@QueryMapping
	public List<Integer> testA() {
		return List.of(1,2,3,4,5,6,7,8,9);
	}
	
	@QueryMapping
	public List<String> testB() {
		return List.of("a", "b", "c", "d", "e", "f", "g");
	}
}
