package net.luversof.web.blog.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.blog.domain.BlogArticle;
import net.luversof.blog.service.BlogArticleService;
import net.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;

@RestController
@RequestMapping(value = "/api/blogArticle", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleController {

	@Autowired
	private BlogArticleService blogArticleService;
	
	@GetMapping("/{blogId}")
	public Page<BlogArticle> getMyBlog(@PathVariable UUID blogId, Pageable pageable) {
		return blogArticleService.findByBlogId(blogId, pageable);
	}
	
	@BlueskyPreAuthorize
	@PostMapping
	public BlogArticle save(BlogArticle blogArticle) {
		return blogArticleService.save(blogArticle);
	}
}
