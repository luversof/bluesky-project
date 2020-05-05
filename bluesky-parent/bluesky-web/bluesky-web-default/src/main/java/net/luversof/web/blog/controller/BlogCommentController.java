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

import net.luversof.blog.domain.BlogComment;
import net.luversof.blog.service.BlogCommentService;
import net.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import net.luversof.web.blog.domain.BlogCommentPageRequest;

@RestController
@RequestMapping(value = "/api/blogComment", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class BlogCommentController {

	@Autowired
	private BlogCommentService blogCommentService;
	
	@GetMapping("/search/findByBlogArticleId/{blogArticleId}")
	public Page<BlogComment> findByBlogArticleId(@PathVariable long blogArticleId, BlogCommentPageRequest blogCommentPageRequest) {
		return blogCommentService.findByBlogArticleId(blogArticleId, blogCommentPageRequest.toPageRequest());
	}
	
	@BlueskyPreAuthorize
	@PostMapping
	public BlogComment create(@RequestBody @Validated(BlogComment.Create.class) BlogComment blogComment) {
		return blogCommentService.create(blogComment);
	}
	
	@BlueskyPreAuthorize
	@PutMapping("/{id}")
	public BlogComment update(@RequestBody @Validated(BlogComment.Update.class) BlogComment blogComment) {
		return blogCommentService.update(blogComment);
	}
	
	@BlueskyPreAuthorize
	@DeleteMapping("/{id}")
	public void delete(@PathVariable long id) {
		blogCommentService.delete(id);
	}

}