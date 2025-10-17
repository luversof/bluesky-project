package net.luversof.web.gate.blog.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import lombok.Setter;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.blog.domain.BlogArticleComment;
import net.luversof.web.gate.blog.openfeign.BlogArticleCommentClient;


@RestController
@RequestMapping(value = "/api/blogArticleComment", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleCommentController {

	@Setter(onMethod_ = @Autowired)
	private BlogArticleCommentClient blogArticleCommentClient;
	
	@BlueskyPreAuthorize
	@PostMapping
	public BlogArticleComment create(@RequestBody BlogArticleComment blogArticleComment) {
		return blogArticleCommentClient.create(blogArticleComment.toBuilder().userId(UserUtil.getUserId().toString()).build());
	}
	
	@GetMapping("/search/findByBlogArticleId/{blogArticleId}")
	public Page<BlogArticleComment> findByBlogArticleId(@PathVariable String blogArticleId, Pageable pageable) {
		return blogArticleCommentClient.findByBlogArticleId(blogArticleId, pageable);
	}
	
	@GetMapping("/search/findByBlogArticleCommentId/{blogArticleCommentId}")
	public Optional<BlogArticleComment> findByBlogArticleCommentId(@PathVariable String blogArticleCommentId) {
		return blogArticleCommentClient.findByBlogArticleCommentId(blogArticleCommentId);
	}
	
	@GetMapping("/search/countByBlogArticleId/{blogArticleId}")
	public long countByBlogArticleId(@PathVariable String blogArticleId) {
		return blogArticleCommentClient.countByBlogArticleId(blogArticleId);
	}
	
	@BlueskyPreAuthorize
	@PutMapping
	public BlogArticleComment update(@RequestBody BlogArticleComment blogArticleComment) {
		return blogArticleCommentClient.update(blogArticleComment.toBuilder().userId(UserUtil.getUserId().toString()).build());
	}
	
	@BlueskyPreAuthorize
	@DeleteMapping
	public void delete(@RequestBody BlogArticleComment blogArticleComment) {
		blogArticleCommentClient.delete(blogArticleComment.toBuilder().userId(UserUtil.getUserId().toString()).build());
	}

}