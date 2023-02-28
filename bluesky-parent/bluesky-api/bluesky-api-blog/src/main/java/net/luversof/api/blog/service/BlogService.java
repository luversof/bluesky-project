package net.luversof.api.blog.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.blog.domain.mariadb.Blog;
import net.luversof.api.blog.repository.mariadb.BlogRepository;

@Service
public class BlogService {
	
	@Autowired
	private BlogRepository blogRepository;

	public Blog create(Blog blog) {
		blog.setBlogId(UUID.randomUUID().toString());
		return blogRepository.save(blog);
	}
	
	public Optional<Blog> findByBlogId(String blogId) {
		return blogRepository.findByBlogId(blogId);
	}

	public List<Blog> findByUserId(String userId) {
		return blogRepository.findByUserId(userId);
	}
	
}
