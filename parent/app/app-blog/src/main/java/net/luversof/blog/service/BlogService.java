package net.luversof.blog.service;

import java.util.List;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlogService {
	
	@Autowired
	private BlogRepository blogRepository;
	
	public void save(Blog blog) {
		blogRepository.save(blog);
	}
	
	public Blog view(Long id) {
		return blogRepository.findOne(id);
	}

	public List<Blog> list() {
		return blogRepository.findAll();
	}
}
