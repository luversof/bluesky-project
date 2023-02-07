package net.luversof.api.blog.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.blog.domain.mariadb.BlogArticleCategory;
import net.luversof.api.blog.service.BlogArticleCategoryService;

@RestController
@RequestMapping(value = "/api/blog/articleCategory", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleCategoryController {

	@Autowired
	private BlogArticleCategoryService blogArticleCategoryService;
	
	@PostMapping
	public BlogArticleCategory create(@RequestBody BlogArticleCategory blogArticleCategory) {
		return blogArticleCategoryService.create(blogArticleCategory);
	}
	
	@GetMapping("/findByBlogId")
	public List<BlogArticleCategory> findByBlogId(@RequestParam String blogId) {
		return blogArticleCategoryService.findByBlogId(blogId);
	}

	@PutMapping
	public BlogArticleCategory update(@RequestBody BlogArticleCategory blogArticleCategory) {
		return blogArticleCategoryService.update(blogArticleCategory);
	}
	
	@DeleteMapping
	public void delete(@RequestBody BlogArticleCategory blogArticleCategory) {
		blogArticleCategoryService.delete(blogArticleCategory);
	}
}
