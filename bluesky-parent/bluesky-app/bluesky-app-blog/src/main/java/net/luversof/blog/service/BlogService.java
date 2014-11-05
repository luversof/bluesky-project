package net.luversof.blog.service;

import java.util.List;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.core.BlueskyException;
import net.luversof.jdbc.datasource.DataSource;
import net.luversof.jdbc.datasource.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.BLOG)
public class BlogService {

	@Autowired
	private BlogRepository blogRepository;
	
	
	public Blog save(long userId, String userType) {
		Blog blog = new Blog();
		blog.setUserId(userId);
		blog.setUserType(userType);
		return blogRepository.save(blog);
	}
	
	@Transactional(readOnly = true)
	public Blog findOne(long id) {
		return blogRepository.findOne(id);
	}

	@Transactional(readOnly = true)
	public List<Blog> findByUser(long userId, String userType) {
		List<Blog> blogList = blogRepository.findByUserIdAndUserType(userId, userType);
		if (blogList.isEmpty()) {
			throw new BlueskyException("blog.notExist");
		}
		return blogList;
	}
}
