package net.luversof.api.blog.controller;

import java.util.Optional;

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

import lombok.Setter;
import net.luversof.api.blog.domain.mariadb.BlogArticle;
import net.luversof.api.blog.service.BlogArticleService;

@RestController
@RequestMapping(value = "/api/blogArticle", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleController {

	@Setter(onMethod_ = @Autowired)
	private BlogArticleService blogArticleService;
	
	@PostMapping
	public BlogArticle create(@Validated(BlogArticle.Create.class) @RequestBody BlogArticle blogArticle) {
		return blogArticleService.create(blogArticle);
	}
	
	@GetMapping("/search/findByBlogId/{blogId}")
	public Page<BlogArticle> findByBlogId(@PathVariable String blogId, Pageable pageable) {
		return blogArticleService.findByBlogId(blogId, pageable);
	}
	
	@GetMapping("/search/findByBlogArticleId/{blogArticleId}")
	public Optional<BlogArticle> findByBlogArticleId(@PathVariable String blogArticleId) {
		return blogArticleService.findByBlogArticleId(blogArticleId);
	}
	
	@PutMapping
	public BlogArticle update(@Validated(BlogArticle.Update.class) @RequestBody BlogArticle blogArticle) {
		return blogArticleService.update(blogArticle);
	}
	
	@DeleteMapping
	public void delete(@Validated(BlogArticle.Delete.class) @RequestBody BlogArticle blogArticle) {
		blogArticleService.delete(blogArticle);
	}
	
}