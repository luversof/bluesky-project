package net.luversof.web.blog.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
	
	@GetMapping("/search/findByBlogId/{blogId}")
	public Page<BlogArticle> getBlogArticleList(@PathVariable UUID blogId, Pageable pageable) {
		return blogArticleService.findByBlogId(blogId, pageable);
	}
	
	@GetMapping("{/{id}")
	public Optional<BlogArticle> findById(@PathVariable long id) {
		return blogArticleService.findById(id);
	}
	
	@BlueskyPreAuthorize
	@PostMapping
	public BlogArticle save(@RequestBody @Validated(BlogArticle.Create.class) BlogArticle blogArticle) {
		return blogArticleService.create(blogArticle);
	}
	
	@BlueskyPreAuthorize
	@PutMapping("/{id}")
	public BlogArticle modify(@RequestBody @Validated(BlogArticle.Update.class) BlogArticle blogArticle) {
		return blogArticleService.update(blogArticle);
	}
	
	@BlueskyPreAuthorize
	@DeleteMapping("/{id}")
	public void delete(@PathVariable long id) {
		blogArticleService.delete(id);
	}
}
