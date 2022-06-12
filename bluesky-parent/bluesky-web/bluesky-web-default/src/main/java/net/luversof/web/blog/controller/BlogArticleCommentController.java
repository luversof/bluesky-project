package net.luversof.web.blog.controller;

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
import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.mysql.BlogArticleComment;
import net.luversof.blog.service.BlogArticleCommentService;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.blog.domain.BlogCommentPageRequest;

@RestController
@RequestMapping(value = "/api/blogArticleComment", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleCommentController {

	@Autowired
	private BlogArticleCommentService blogArticleCommentService;
	
	@GetMapping("/search/findByBlogArticleId/{blogArticleId}")
	public Page<BlogArticleComment> findByBlogArticleId(@PathVariable String blogArticleId, BlogCommentPageRequest blogCommentPageRequest) {
		return blogArticleCommentService.findByBlogArticleId(blogArticleId, blogCommentPageRequest.toPageRequest());
	}
	
	@BlueskyPreAuthorize
	@PostMapping
	public BlogArticleComment create(@RequestBody @Validated(BlogArticleComment.Create.class) BlogArticleComment blogComment) {
		var savedBlogComment = blogArticleCommentService.create(blogComment);
		// TODO comment 조회수 증가
		return savedBlogComment;
	}
	
	@BlueskyPreAuthorize
	@PutMapping("/{blogArticleCommentId}")
	public BlogArticleComment update(@RequestBody @Validated(BlogArticleComment.Update.class) BlogArticleComment blogComment) {
		return blogArticleCommentService.update(blogComment);
	}
	
	@BlueskyPreAuthorize
	@DeleteMapping("/{blogArticleCommentId}")
	public void deleteByBlogArticleCommentId(@PathVariable String blogArticleCommentId, BlueskyUser blueskyUser) {
		var blogArticleComment = blogArticleCommentService.findByBlogArticleCommentId(blogArticleCommentId).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGCOMMENT));
		if (!blogArticleComment.getUserId().equals(blueskyUser.getId())) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGCOMMENT);
		}
		blogArticleCommentService.deleteByBlogArticleCommentId(blogArticleCommentId);
	}

}