package net.luversof.blog.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;

@Service
@Transactional("blogTransactionManager")
public class BlogService {

	@Autowired
	private BlogRepository blogRepository;
	
	
	public Blog save(Blog blog) {
		return blogRepository.save(blog);
	}
	
	@Transactional(value = "blogTransactionManager", readOnly = true)
	public Blog findOne(long blogId) {
		return blogRepository.findOne(blogId);
	}

	@Transactional(value = "blogTransactionManager", readOnly = true)
	public Blog findByUser(long userId) {
		return blogRepository.findByUserId(userId);
	}
}
