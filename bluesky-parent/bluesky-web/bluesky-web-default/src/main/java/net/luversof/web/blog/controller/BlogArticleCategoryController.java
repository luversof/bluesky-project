package net.luversof.web.blog.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.blog.domain.BlogArticleCategory;
import net.luversof.blog.service.BlogArticleCategoryService;
import net.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;

@RestController
@RequestMapping(value = "/api/blogArticleCategory", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleCategoryController {

	@Autowired
	private BlogArticleCategoryService blogArticleCategoryService;
	
	@BlueskyPreAuthorize
	@GetMapping("/userBlogArticleCategoryList")
	public List<BlogArticleCategory> userBlogArticleCategoryList() {
		return blogArticleCategoryService.findByUserBlogId();
	}

}
