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
import net.luversof.web.gate.blog.client.BlogArticleCommentClient;
import net.luversof.web.gate.blog.domain.BlogArticleComment;
import net.luversof.web.gate.user.util.UserUtil;


@RestController
@RequestMapping(value = "/api/blog/articleComment", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleCommentController {

	@Autowired
	private BlogArticleCommentClient blogArticleCommentClient;
	
	@BlueskyPreAuthorize
	@PostMapping
	public BlogArticleComment create(@RequestBody BlogArticleComment blogArticleComment) {
		return blogArticleCommentClient.create(blogArticleComment.toBuilder().userId(UserUtil.getUserId()).build());
	}
	
	@GetMapping("/findByBlogArticleId")
	public Page<BlogArticleComment> findByBlogArticleId(@RequestParam String blogArticleId, Pageable pageable) {
		return blogArticleCommentClient.findByBlogArticleId(blogArticleId, pageable);
	}
	
	@GetMapping("/findByBlogArticleCommentId")
	public Optional<BlogArticleComment> findByBlogArticleCommentId(@RequestParam String blogArticleCommentId) {
		return blogArticleCommentClient.findByBlogArticleCommentId(blogArticleCommentId);
	}
	
	@GetMapping("/countByBlogArticleId")
	public long countByBlogArticleId(@RequestParam String blogArticleId) {
		return blogArticleCommentClient.countByBlogArticleId(blogArticleId);
	}
	
	@BlueskyPreAuthorize
	@PutMapping
	public BlogArticleComment update(@RequestBody BlogArticleComment blogArticleComment) {
		return blogArticleCommentClient.update(blogArticleComment.toBuilder().userId(UserUtil.getUserId()).build());
	}
	
	@BlueskyPreAuthorize
	@DeleteMapping
	public void delete(@RequestBody BlogArticleComment blogArticleComment) {
		blogArticleCommentClient.delete(blogArticleComment.toBuilder().userId(UserUtil.getUserId()).build());
	}

}