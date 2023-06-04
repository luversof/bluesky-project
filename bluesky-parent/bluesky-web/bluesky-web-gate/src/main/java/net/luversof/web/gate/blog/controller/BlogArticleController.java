package net.luversof.web.gate.blog.controller;

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

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.web.gate.blog.client.BlogArticleClient;
import net.luversof.web.gate.blog.domain.BlogArticle;
import net.luversof.web.gate.user.util.UserUtil;

@RestController
@RequestMapping(value = "/api/blog/article", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleController {

	@Autowired
	private BlogArticleClient blogArticleClient;
	
	@BlueskyPreAuthorize
	@PostMapping
	public BlogArticle create(@RequestBody BlogArticle blogArticle) {
		return blogArticleClient.create(blogArticle.toBuilder().userId(UserUtil.getUserId()).build());
	}
	
	@GetMapping("/findByBlogId")
	public Page<BlogArticle> findByBlogId(@RequestParam String blogId, Pageable pageable) {
		return blogArticleClient.findByBlogId(blogId, pageable);
	}
	
	@GetMapping("/findByBlogArticleId")
	public Optional<BlogArticle> findByBlogArticleId(@RequestParam String blogArticleId) {
		return blogArticleClient.findByBlogArticleId(blogArticleId);
	}
	
	@BlueskyPreAuthorize
	@PutMapping
	public BlogArticle update(@RequestBody BlogArticle blogArticle) {
		return blogArticleClient.update(blogArticle.toBuilder().userId(UserUtil.getUserId()).build());
	}
	
	@BlueskyPreAuthorize
	@DeleteMapping
	public void delete(@RequestBody BlogArticle blogArticle) {
		blogArticleClient.delete(blogArticle.toBuilder().userId(UserUtil.getUserId()).build());
	}

}