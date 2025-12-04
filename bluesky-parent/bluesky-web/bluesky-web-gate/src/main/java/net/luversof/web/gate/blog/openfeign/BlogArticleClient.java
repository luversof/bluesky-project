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
//import net.luversof.web.gate.blog.domain.BlogArticle;
//
//@FeignClient(value = "bluesky-api-blog", contextId = "api-blog-article", path = "/api/blogArticle", url = "${gate.feign-client.url.blog:}")
//public interface BlogArticleClient {
//
//	@PostMapping
//	BlogArticle create(@RequestBody BlogArticle blogArticle);
//	
//	@GetMapping("/search/findByBlogId/{blogId}")
//	Page<BlogArticle> findByBlogId(@PathVariable String blogId, Pageable pageable);
//	
//	@GetMapping("/search/findByBlogArticleId/{blogArticleId}")
//	Optional<BlogArticle> findByBlogArticleId(@PathVariable String blogArticleId);
//	
//	@PutMapping
//	BlogArticle update(@RequestBody BlogArticle blogArticle);
//	
//	@DeleteMapping
//	void delete(@RequestBody BlogArticle blogArticle);
//
//}