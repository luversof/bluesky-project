package net.luversof.web.blog.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.service.BlogService;
import net.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;

@RestController
@RequestMapping(value = "/api/blog", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class BlogController {

	@Autowired
	private BlogService blogService;
	
	@BlueskyPreAuthorize
	@GetMapping("/userBlog")
	public Optional<Blog> userBlog() {
		return blogService.findByUserId();
	}
}
