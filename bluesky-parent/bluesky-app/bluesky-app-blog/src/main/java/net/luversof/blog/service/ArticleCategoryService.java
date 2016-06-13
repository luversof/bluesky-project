package net.luversof.blog.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.blog.domain.ArticleCategory;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.ArticleCategoryRepository;

@Service
public class ArticleCategoryService {

	@Autowired
	private ArticleCategoryRepository articleCategoryRepository;

	public ArticleCategory save(ArticleCategory articleCategory) {
		return articleCategoryRepository.save(articleCategory);
	}

	public ArticleCategory findOne(long id) {
		return articleCategoryRepository.findOne(id);
	}

	public List<ArticleCategory> findByBlog(Blog blog) {
		return articleCategoryRepository.findByBlog(blog);
	}

	public void delete(long id) {
		articleCategoryRepository.delete(id);
	}
}
