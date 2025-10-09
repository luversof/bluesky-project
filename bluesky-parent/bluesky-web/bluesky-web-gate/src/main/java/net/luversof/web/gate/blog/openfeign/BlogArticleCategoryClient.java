package net.luversof.web.gate.blog.openfeign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import net.luversof.web.gate.blog.domain.BlogArticleCategory;


@FeignClient(value = "bluesky-api-blog", contextId = "api-blog-articleCategory", path = "/api/blogArticleCategory", url = "${gate.feign-client.url.blog:}")
public interface BlogArticleCategoryClient {
	
	@PostMapping
	BlogArticleCategory create(@RequestBody BlogArticleCategory blogArticleCategory);
	
	@GetMapping("/search/findByBlogId/{blogId}")
	List<BlogArticleCategory> findByBlogId(@PathVariable String blogId);

	@PutMapping
	BlogArticleCategory update(@RequestBody BlogArticleCategory blogArticleCategory);
	
	@DeleteMapping
	void delete(@RequestBody BlogArticleCategory blogArticleCategory);

}