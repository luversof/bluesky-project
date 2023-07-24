package net.luversof.api.blog.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.blog.controller.swagger.BlogControllerOperation;
import net.luversof.api.blog.domain.mariadb.Blog;
import net.luversof.api.blog.service.BlogService;

@RestController
@RequestMapping(value = "/api/blog", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogController {

	@Autowired
	private BlogService blogService;
	
	@PostMapping
	@BlogControllerOperation.Create
	public Blog create(@Validated(Blog.Create.class) @RequestBody Blog blog) {
		return blogService.create(blog);
	}
	
	@GetMapping("/findByBlogId")
	public Optional<Blog> findByBlogId(@RequestParam String blogId) {
		return blogService.findByBlogId(blogId);
	}
	
	@GetMapping("/findByUserId")
	public List<Blog> findByUserId(@RequestParam String userId) {
		return blogService.findByUserId(userId);
	}

}