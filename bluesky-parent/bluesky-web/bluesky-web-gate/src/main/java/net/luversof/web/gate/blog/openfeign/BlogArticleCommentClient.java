//package net.luversof.web.gate.blog.openfeign;
//
//import java.util.Optional;
//
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//
//import net.luversof.web.gate.blog.domain.BlogArticleComment;
//
//
//@FeignClient(value = "bluesky-api-blog", contextId = "api-blog-articleComment", path = "/api/blogArticleComment", url = "${gate.feign-client.url.blog:}")
//public interface BlogArticleCommentClient {
//	
//	@PostMapping
//	BlogArticleComment create(@RequestBody BlogArticleComment blogArticleComment);
//	
//	@GetMapping("/search/findByBlogArticleId/{blogArticleId}")
//	Page<BlogArticleComment> findByBlogArticleId(@PathVariable String blogArticleId, Pageable pageable);
//	
//	@GetMapping("/search/findByBlogArticleCommentId/{blogArticleCommentId}")
//	Optional<BlogArticleComment> findByBlogArticleCommentId(@PathVariable String blogArticleCommentId);
//	
//	@GetMapping("/search/countByBlogArticleId/{blogArticleId}")
//	long countByBlogArticleId(@PathVariable String blogArticleId);
//	
//	@PutMapping
//	BlogArticleComment update(@RequestBody BlogArticleComment blogArticleComment);
//	
//	@DeleteMapping
//	void delete(@RequestBody BlogArticleComment blogArticleComment);
//
//}