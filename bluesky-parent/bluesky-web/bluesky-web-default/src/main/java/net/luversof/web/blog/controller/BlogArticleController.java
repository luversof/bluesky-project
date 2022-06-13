package net.luversof.web.blog.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

import io.github.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import net.luversof.blog.domain.mysql.BlogArticle;
import net.luversof.blog.service.BlogArticleService;
import net.luversof.web.blog.domain.BlogArticlePageRequest;

@RestController
@RequestMapping(value = "/api/blogArticle", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleController {

	@Autowired
	private BlogArticleService blogArticleService;
	
	@GetMapping("/search/findByBlogId/{blogId}")
	public Page<BlogArticle> findByBlogId(@PathVariable String blogId, BlogArticlePageRequest blogArticlePageRequest) {
		return blogArticleService.findByBlogId(blogId, blogArticlePageRequest.toPageRequest());
	}
	
	
	@GetMapping("/{blogArticleId}")
	public Optional<BlogArticle> findByBlogArticleId(@PathVariable String blogArticleId) {
		var savedBlogArticle = blogArticleService.findByBlogArticleId(blogArticleId);
		// TODO blogArticle 조회수 증가 처리
		return savedBlogArticle;
	}
	
//	@BlueskyPreAuthorize
	@PostMapping
	public BlogArticle create(@RequestBody @Validated(BlogArticle.Create.class) BlogArticle blogArticle) {
		return blogArticleService.create(blogArticle);
	}
	
	@BlueskyPreAuthorize
	@PutMapping("/{blogArticleId}")
	public BlogArticle update(@RequestBody @Validated(BlogArticle.Update.class) BlogArticle blogArticle) {
		return blogArticleService.update(blogArticle);
	}
	
	@BlueskyPreAuthorize
	@DeleteMapping("/{blogArticleId}")
	public void delete(@RequestBody @Validated(BlogArticle.Delete.class) BlogArticle blogArticle) {
		blogArticleService.delete(blogArticle);
	}

}
