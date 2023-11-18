package net.luversof.web.gate.feign.blog.client;

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

import net.luversof.web.gate.feign.blog.domain.BlogArticleComment;


@FeignClient(value = "bluesky-api-blog", contextId = "api-blog-articleComment", path = "/api/blog/articleComment", url = "${gate.feign-client.url.blog:}")
public interface BlogArticleCommentClient {
	
	@PostMapping
	BlogArticleComment create(@RequestBody BlogArticleComment blogArticleComment);
	
	@GetMapping("/findByBlogArticleId")
	Page<BlogArticleComment> findByBlogArticleId(@RequestParam String blogArticleId, Pageable pageable);
	
	@GetMapping("/findByBlogArticleCommentId")
	Optional<BlogArticleComment> findByBlogArticleCommentId(@RequestParam String blogArticleCommentId);
	
	@GetMapping("/countByBlogArticleId")
	long countByBlogArticleId(@RequestParam String blogArticleId);
	
	@PutMapping
	BlogArticleComment update(@RequestBody BlogArticleComment blogArticleComment);
	
	@DeleteMapping
	void delete(@RequestBody BlogArticleComment blogArticleComment);

}