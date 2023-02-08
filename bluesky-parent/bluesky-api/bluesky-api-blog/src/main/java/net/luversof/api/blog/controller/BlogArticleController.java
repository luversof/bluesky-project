package net.luversof.api.blog.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.blog.domain.mariadb.BlogArticle;
import net.luversof.api.blog.service.BlogArticleService;

@RestController
@RequestMapping(value = "/api/blog/article", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleController {

	@Autowired
	private BlogArticleService blogArticleService;
	
	@PostMapping
	public BlogArticle create(@RequestBody BlogArticle blogArticle) {
		return blogArticleService.create(blogArticle);
	}
	
	@GetMapping("/findByBlogId")
	public Page<BlogArticle> findByBlogId(@RequestParam String blogId, Pageable pageable) {
		return blogArticleService.findByBlogId(blogId, pageable);
	}
	
	@GetMapping("/findByBlogArticleId")
	public Optional<BlogArticle> findByBlogArticleId(@RequestParam String blogArticleId) {
		return blogArticleService.findByBlogArticleId(blogArticleId);
	}
	
	@PutMapping
	public BlogArticle update(@RequestBody BlogArticle blogArticle) {
		return blogArticleService.update(blogArticle);
	}
	
	@DeleteMapping
	public void delete(@RequestBody BlogArticle blogArticle) {
		blogArticleService.delete(blogArticle);
	}
	
}