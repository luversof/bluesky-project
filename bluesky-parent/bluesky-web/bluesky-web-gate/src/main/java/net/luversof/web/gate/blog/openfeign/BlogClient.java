package net.luversof.web.gate.blog.openfeign;

import java.util.List;
import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import net.luversof.web.gate.blog.domain.Blog;

@FeignClient(value = "bluesky-api-blog", contextId = "api-blog", path = "/api/blog", url = "${gate.feign-client.url.blog:}")
public interface BlogClient {

	@PostMapping
	Blog create(@RequestBody Blog blog);
	
	@GetMapping("/search/findByBlogId/{blogId}")
	Optional<Blog> findByBlogId(@PathVariable String blogId);
	
	@GetMapping("/search/findByUserId/{userId}")
	List<Blog> findByUserId(@PathVariable String userId);

}