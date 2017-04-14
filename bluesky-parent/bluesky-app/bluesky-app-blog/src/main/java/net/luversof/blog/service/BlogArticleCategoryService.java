package net.luversof.blog.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.blog.domain.BlogArticleCategory;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogArticleCategoryRepository;

@Service
public class BlogArticleCategoryService {

	@Autowired
	private BlogArticleCategoryRepository articleCategoryRepository;

	public BlogArticleCategory save(BlogArticleCategory articleCategory) {
		return articleCategoryRepository.save(articleCategory);
	}

	public BlogArticleCategory findOne(long id) {
		return articleCategoryRepository.findOne(id);
	}

	public List<BlogArticleCategory> findByBlog(Blog blog) {
		return articleCategoryRepository.findByBlog(blog);
	}

	public void delete(long id) {
		articleCategoryRepository.delete(id);
	}
}
