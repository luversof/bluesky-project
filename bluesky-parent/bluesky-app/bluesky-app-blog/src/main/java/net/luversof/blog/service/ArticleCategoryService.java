package net.luversof.blog.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.ArticleCategory;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.ArticleCategoryRepository;

@Service
@Transactional("blogTransactionManager")
public class ArticleCategoryService {

	@Autowired
	private ArticleCategoryRepository articleCategoryRepository;

	public ArticleCategory save(ArticleCategory articleCategory) {
		return articleCategoryRepository.save(articleCategory);
	}

	@Transactional(value = "blogTransactionManager", readOnly = true)
	public ArticleCategory findOne(long id) {
		return articleCategoryRepository.findOne(id);
	}

	@Transactional(value = "blogTransactionManager", readOnly = true)
	public List<ArticleCategory> findByBlog(Blog blog) {
		return articleCategoryRepository.findByBlog(blog);
	}

	public void delete(long id) {
		articleCategoryRepository.delete(id);
	}
}
