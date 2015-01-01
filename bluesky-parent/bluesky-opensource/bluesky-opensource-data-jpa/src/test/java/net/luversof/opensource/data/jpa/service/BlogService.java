package net.luversof.opensource.data.jpa.service;

import java.util.List;

















import net.luversof.core.BlueskyException;
import net.luversof.opensource.data.jpa.domain.Blog;
import net.luversof.opensource.data.jpa.repository.BlogRepository;
import net.luversof.opensource.jdbc.routing.DataSource;
import net.luversof.opensource.jdbc.routing.DataSourceType;

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
	public Blog findOne(long blogId) {
		return blogRepository.findOne(blogId);
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
