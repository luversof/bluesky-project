package net.luversof.blog.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.blog.util.BlogRequestAttributeUtil;

@RepositoryRestController
public class BlogRepositoryRestController {

	@Autowired
	private BlogRepository blogRepository;

	@GetMapping(value = "/blogs/search/myBlog")
	public @ResponseBody ResponseEntity<?> getProducers() {
		UUID userId = BlogRequestAttributeUtil.getUserId();
		if (userId == null) {
			return null;
		}
		Blog blog = blogRepository.findByUserId(userId).orElse(null);
		Resource<Blog> resources = new Resource<Blog>(blog);
		return ResponseEntity.ok(resources);
	}

}