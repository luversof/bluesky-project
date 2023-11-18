package net.luversof.web.gate.feign.blog.client;

import java.util.List;
import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.feign.blog.domain.Blog;

@FeignClient(value = "bluesky-api-blog", contextId = "api-blog", path = "/api/blog", url = "${gate.feign-client.url.blog:}")
public interface BlogClient {

	@PostMapping
	Blog create(@RequestBody Blog blog);
	
	@GetMapping("/findByBlogId")
	Optional<Blog> findByBlogId(@RequestParam String blogId);
	
	@GetMapping("/findByUserId")
	List<Blog> findByUserId(@RequestParam String userId);

}