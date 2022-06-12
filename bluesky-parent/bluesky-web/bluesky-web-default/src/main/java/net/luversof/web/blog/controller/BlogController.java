package net.luversof.web.blog.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.service.BlogService;
import net.luversof.security.core.userdetails.BlueskyUser;

@RestController
@RequestMapping(value = "/api/blog", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class BlogController {

	@Autowired
	private BlogService blogService;
	
	@BlueskyPreAuthorize
	@GetMapping("/userBlogList")
	public List<Blog> userBlogList(BlueskyUser blueskyUser) {
		return blogService.findByUserId(blueskyUser.getId());
	}
}
