package net.luversof.web.gate.blog.client;

import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.blog.domain.BlogArticle;

@FeignClient(value = "bluesky-api-blog", contextId = "api-blog-article", path = "/api/blog/article", url = "${gate.feign-client.url.blog:}")
public interface BlogArticleClient {

	@PostMapping
	BlogArticle create(@RequestBody BlogArticle blogArticle);
	
	@GetMapping("/findByBlogId")
	Page<BlogArticle> findByBlogId(@RequestParam String blogId, Pageable pageable);
	
	@GetMapping("/findByBlogArticleId")
	Optional<BlogArticle> findByBlogArticleId(@RequestParam String blogArticleId);
	
	@PutMapping
	BlogArticle update(@RequestBody BlogArticle blogArticle);
	
	@DeleteMapping
	void delete(@RequestBody BlogArticle blogArticle);
}
