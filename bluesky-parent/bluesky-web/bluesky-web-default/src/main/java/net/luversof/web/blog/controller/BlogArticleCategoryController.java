package net.luversof.web.blog.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import net.luversof.blog.domain.mysql.BlogArticleCategory;
import net.luversof.blog.service.BlogArticleCategoryService;
import net.luversof.blog.service.BlogService;
import net.luversof.security.core.userdetails.BlueskyUser;

@RestController
@RequestMapping(value = "/api/blogArticleCategory", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleCategoryController {

	@Autowired
	private BlogArticleCategoryService blogArticleCategoryService;
	
	@Autowired
	private BlogService blogService;
	
	@BlueskyPreAuthorize
	@GetMapping("/search/findByUserBlogId")
	public List<BlogArticleCategory> findByUserBlogId(String blogId, BlueskyUser blueskyUser) {
		var blog = blogService.findByBlogId(blogId).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOG));
		if (!blog.getUserId().equals(blueskyUser.getId())) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOG);
		}
		
		return blogArticleCategoryService.findByBlogId(blogId);
	}

	@BlueskyPreAuthorize
	@PostMapping
	public BlogArticleCategory create(@RequestBody @Validated(BlogArticleCategory.Create.class) BlogArticleCategory blogArticleCategory) {
		return blogArticleCategoryService.create(blogArticleCategory);
	}
	
	@BlueskyPreAuthorize
	@PutMapping("/{blogArticleCategoryId}")
	public BlogArticleCategory update(@RequestBody @Validated(BlogArticleCategory.Update.class) BlogArticleCategory blogArticleCategory) {
		return blogArticleCategoryService.update(blogArticleCategory);
	}
	
	@BlueskyPreAuthorize
	@DeleteMapping("/{blogArticleCategoryId}")
	public void delete(@PathVariable String blogArticleCategoryId) {
		blogArticleCategoryService.deleteByBlogArticleCategoryId(blogArticleCategoryId);
	}
}
