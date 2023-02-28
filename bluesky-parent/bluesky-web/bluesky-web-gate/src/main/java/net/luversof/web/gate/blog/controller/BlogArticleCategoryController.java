package net.luversof.web.gate.blog.controller;

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

import net.luversof.web.gate.blog.client.BlogArticleCategoryClient;
import net.luversof.web.gate.blog.domain.BlogArticleCategory;

@RestController
@RequestMapping(value = "/api/blog/articleCategory", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleCategoryController {

	@Autowired
	private BlogArticleCategoryClient blogArticleCategoryClient;
	
	@PostMapping
	public BlogArticleCategory create(@RequestBody BlogArticleCategory blogArticleCategory) {
		return blogArticleCategoryClient.create(blogArticleCategory);
	}
	
	@GetMapping("/findByBlogId")
	public List<BlogArticleCategory> findByBlogId(@RequestParam String blogId) {
		return blogArticleCategoryClient.findByBlogId(blogId);
	}

	@PutMapping
	public BlogArticleCategory update(@RequestBody BlogArticleCategory blogArticleCategory) {
		return blogArticleCategoryClient.update(blogArticleCategory);
	}
	
	@DeleteMapping
	public void delete(@RequestBody BlogArticleCategory blogArticleCategory) {
		blogArticleCategoryClient.delete(blogArticleCategory);
	}

}