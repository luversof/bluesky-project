package net.luversof.blog.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.core.exception.BlueskyException;

@Service
@Transactional("blogTransactionManager")
public class BlogService {

	@Autowired
	private BlogRepository blogRepository;
	
	
	public Blog save(long userId, String userType) {
		Blog blog = new Blog();
		blog.setUserId(userId);
		blog.setUserType(userType);
		return blogRepository.save(blog);
	}
	
	@Transactional(value = "blogTransactionManager", readOnly = true)
	public Blog findOne(long blogId) {
		return blogRepository.findOne(blogId);
	}

	@Transactional(value = "blogTransactionManager", readOnly = true)
	public List<Blog> findByUser(long userId, String userType) {
		List<Blog> blogList = blogRepository.findByUserIdAndUserType(userId, userType);
		if (blogList.isEmpty()) {
			throw new BlueskyException("blog.notExist");
		}
		return blogList;
	}
}