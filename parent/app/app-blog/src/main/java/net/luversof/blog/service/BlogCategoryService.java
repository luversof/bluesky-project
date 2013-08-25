package net.luversof.blog.service;

import net.luversof.blog.domain.BlogCategory;
import net.luversof.blog.repository.BlogCategoryRepository;


import net.luversof.core.datasource.DataSource;
import net.luversof.core.datasource.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.BLOG)
public class BlogCategoryService {
	@Autowired
	private BlogCategoryRepository blogCategoryRepository;

	public BlogCategory save(BlogCategory blogCategory) {
		return blogCategoryRepository.save(blogCategory);
	}

	@Transactional(readOnly = true)
	public BlogCategory view(long id) {
		return blogCategoryRepository.findOne(id);
	}

	@Transactional
	public void delete(long id) {
		blogCategoryRepository.delete(id);
	}
}
