package net.luversof.blog.service;

import java.util.List;

import net.luversof.blog.domain.ArticleCategory;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.ArticleCategoryRepository;




import net.luversof.opensource.jdbc.routing.DataSource;
import net.luversof.opensource.jdbc.routing.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.BLOG)
public class ArticleCategoryService {
	@Autowired
	private ArticleCategoryRepository articleCategoryRepository;

	public ArticleCategory save(ArticleCategory articleCategory) {
		return articleCategoryRepository.save(articleCategory);
	}

	@Transactional(readOnly = true)
	public ArticleCategory findOne(long id) {
		return articleCategoryRepository.findOne(id);
	}
	
	@Transactional(readOnly = true)
	public List<ArticleCategory> findByBlog(Blog blog) {
		return articleCategoryRepository.findByBlog(blog);
	}

	@Transactional
	public void delete(long id) {
		articleCategoryRepository.delete(id);
	}
}
