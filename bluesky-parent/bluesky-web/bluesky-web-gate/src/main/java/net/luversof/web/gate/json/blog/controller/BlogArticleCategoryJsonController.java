package net.luversof.web.gate.json.blog.controller;

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

import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.web.gate.feign.blog.client.BlogArticleCategoryClient;
import net.luversof.web.gate.feign.blog.client.BlogClient;
import net.luversof.web.gate.feign.blog.domain.BlogArticleCategory;
import net.luversof.web.gate.user.util.UserUtil;

@RestController
@RequestMapping(value = "/json/blog/articleCategory", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleCategoryJsonController {

	@Autowired
	private BlogArticleCategoryClient blogArticleCategoryClient;
	
	@Autowired
	private BlogClient blogClient;
	
	@BlueskyPreAuthorize
	@PostMapping
	public BlogArticleCategory create(@RequestBody BlogArticleCategory blogArticleCategory) {
		checkUserBlog(blogArticleCategory);
		return blogArticleCategoryClient.create(blogArticleCategory);
	}
	
	@GetMapping("/findByBlogId")
	public List<BlogArticleCategory> findByBlogId(@RequestParam String blogId) {
		return blogArticleCategoryClient.findByBlogId(blogId);
	}

	@BlueskyPreAuthorize
	@PutMapping
	public BlogArticleCategory update(@RequestBody BlogArticleCategory blogArticleCategory) {
		checkUserBlog(blogArticleCategory);
		return blogArticleCategoryClient.update(blogArticleCategory);
	}
	
	@BlueskyPreAuthorize
	@DeleteMapping
	public void delete(@RequestBody BlogArticleCategory blogArticleCategory) {
		checkUserBlog(blogArticleCategory);
		blogArticleCategoryClient.delete(blogArticleCategory);
	}

	private void checkUserBlog(BlogArticleCategory blogArticleCategory) {
		var userBlogList = blogClient.findByUserId(UserUtil.getUserId());
		if (userBlogList.stream().noneMatch(blog -> blog.blogId().equals(blogArticleCategory.blogId()))) {
			throw new BlueskyException("NOT_USER_BLOG");
		}
	}
}