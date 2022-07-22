package net.luversof.web.blog.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.service.BlogService;
import net.luversof.security.util.SecurityUtil;

@RestController
@RequestMapping(value = "/api/blog", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class BlogController {

	@Autowired
	private BlogService blogService;
	
	@BlueskyPreAuthorize
	@GetMapping("/userBlogList")
	public List<Blog> userBlogList() {
		return blogService.findByUserId(SecurityUtil.getBlueskyUser().getId());
	}
	
	@PostMapping
	public Blog createBlog() {
		List<Blog> userBlogList = blogService.findByUserId(SecurityUtil.getBlueskyUser().getId());
		if (userBlogList != null && !userBlogList.isEmpty()) {
			throw new BlueskyException(BlogErrorCode.ALREADY_EXIST_USER_BLOG);
		}
		return blogService.createBlog(null);
	}
}
