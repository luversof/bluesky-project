package net.luversof.api.blog.controller;

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

import lombok.Setter;
import net.luversof.api.blog.domain.mariadb.BlogArticleCategory;
import net.luversof.api.blog.service.BlogArticleCategoryService;

@RestController
@RequestMapping(value = "/api/blogArticleCategory", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleCategoryController {

	@Setter(onMethod_ = @Autowired)
	private BlogArticleCategoryService blogArticleCategoryService;
	
	@PostMapping
	public BlogArticleCategory create(@Validated(BlogArticleCategory.Create.class) @RequestBody BlogArticleCategory blogArticleCategory) {
		return blogArticleCategoryService.create(blogArticleCategory);
	}
	
	@GetMapping("/search/findByBlogId/{blogId}")
	public List<BlogArticleCategory> findByBlogId(@PathVariable String blogId) {
		return blogArticleCategoryService.findByBlogId(blogId);
	}

	@PutMapping
	public BlogArticleCategory update(@Validated(BlogArticleCategory.Update.class) @RequestBody BlogArticleCategory blogArticleCategory) {
		return blogArticleCategoryService.update(blogArticleCategory);
	}
	
	@DeleteMapping
	public void delete(@Validated(BlogArticleCategory.Delete.class) @RequestBody BlogArticleCategory blogArticleCategory) {
		blogArticleCategoryService.delete(blogArticleCategory);
	}

}