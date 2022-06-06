package net.luversof.blog.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.domain.mysql.TestAnnotation;
import net.luversof.blog.repository.mysql.BlogRepository;

@Service
public class BlogService {
	
	@Autowired
	private BlogRepository blogRepository;
	
	public Optional<Blog> findByUserId(String userId) {
		return blogRepository.findByUserId(userId);
	}

	public Blog createBlog(@TestAnnotation(Blog.Create.class) Blog blog) {
		blog.setBlogId(UUID.randomUUID().toString());
		return blogRepository.save(blog);
	}
	
	public Blog createBlog(@TestAnnotation(Blog.Create.class) Blog blog, String a, String b) {
		blog.setBlogId(UUID.randomUUID().toString());
		return blogRepository.save(blog);
	}
	
	public Blog createBlog(String a, String b, @TestAnnotation(Blog.Create.class) Blog blog) {
		blog.setBlogId(UUID.randomUUID().toString());
		return blogRepository.save(blog);
	}
}
