package net.luversof.web.gate.blog.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.web.gate.blog.client.BlogClient;
import net.luversof.web.gate.blog.domain.Blog;
import net.luversof.web.gate.user.util.UserUtil;

@RestController
@RequestMapping(value = "/api/blog", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogController {

	@Autowired
	private BlogClient blogClient;
	
	@BlueskyPreAuthorize
	@PostMapping
	public Blog create() {
		return blogClient.create(Blog.builder().userId(UserUtil.getUserId()).build());
	}
	
	@GetMapping("/findByBlogId")
	public Optional<Blog> findByBlogId(@RequestParam String blogId) {
		return blogClient.findByBlogId(blogId);
	}
	
	@GetMapping("/findByUserId")
	public List<Blog> findByUserId(@RequestParam String userId) {
		return blogClient.findByUserId(userId);
	}
}
