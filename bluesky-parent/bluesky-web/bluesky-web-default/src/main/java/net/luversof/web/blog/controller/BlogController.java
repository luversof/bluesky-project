package net.luversof.web.blog.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.BlogService;

@RestController
@RequestMapping(value = "/blog", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogController {
	
	@Autowired
	private BlogService blogService;
	
	@GetMapping("/{id}")
	public Optional<Blog> findById(@PathVariable UUID id) {
		return blogService.findById(id);
	}
	
	@PostMapping
	public Blog create() {
		return blogService.create();
	}
}
