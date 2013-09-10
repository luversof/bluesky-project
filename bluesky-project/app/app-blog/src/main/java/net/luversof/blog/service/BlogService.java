package net.luversof.blog.service;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.core.datasource.DataSource;
import net.luversof.core.datasource.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.BLOG)
public class BlogService {

	private static final int PAGE_SIZE = 10;

	@Autowired
	private BlogRepository blogRepository;

	public Blog save(Blog blog) {
		return blogRepository.save(blog);
	}

	@Transactional(readOnly = true)
	public Blog findOne(long id) {
		return blogRepository.findOne(id);
	}

	@Transactional(readOnly = true)
	public Page<Blog> findAll(int page) {
		Sort sort = new Sort(Sort.Direction.DESC, "id");
		PageRequest pageRequest = new PageRequest(page, PAGE_SIZE, sort);
		return blogRepository.findAll(pageRequest);
	}

	public void delete(long id) {
		blogRepository.delete(id);
	}
}
