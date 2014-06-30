package net.luversof.blog.service;

import java.util.List;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;
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
	
	@Transactional(readOnly = true)
	public Blog findOne(long id) {
		return blogRepository.findOne(id);
	}

	@Transactional(readOnly = true)
	public List<Blog> findByUserId(long userId) {
		return blogRepository.findByUserId(userId);
	}
}
