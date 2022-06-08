package net.luversof.blog.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.autoconfigure.validation.annotation.BlueskyValidated;
import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.repository.mysql.BlogRepository;

@Service
public class BlogService {
	
	@Autowired
	private BlogRepository blogRepository;
	
	public List<Blog> findByUserId(String userId) {
		return blogRepository.findByUserId(userId);
	}
	
	public Optional<Blog> findByBlogId(String blogId) {
		return blogRepository.findByBlogId(blogId);
	}

	public Blog createBlog(@BlueskyValidated(Blog.Create.class) Blog blog) {
		blog.setBlogId(UUID.randomUUID().toString());
		return blogRepository.save(blog);
	}

}
