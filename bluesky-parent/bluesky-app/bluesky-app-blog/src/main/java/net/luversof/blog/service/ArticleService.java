package net.luversof.blog.service;

import net.luversof.blog.domain.Article;
import net.luversof.blog.repository.ArticleRepository;
import net.luversof.core.BlueskyException;
import net.luversof.jdbc.datasource.DataSource;
import net.luversof.jdbc.datasource.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.BLOG)
public class ArticleService {

	private static final int PAGE_SIZE = 10;

	@Autowired
	private ArticleRepository articleRepository;
	
	@Autowired
	private ArticleCategoryService blogCategoryService;

	public Article save(Article blog) {
		return articleRepository.save(blog);
	}
	
	public Article update(Article blog) {
		Article targetBlog = findOne(blog.getId());
		if (!blog.getBlog().equals(targetBlog.getBlog())) {
			throw new BlueskyException("userId is not owner");
		}
		targetBlog.setTitle(blog.getTitle());
		targetBlog.setContent(blog.getContent());
		if (blog.getArticleCategory() != null && blog.getArticleCategory().getId() != 0) {
			targetBlog.setArticleCategory(blogCategoryService.findOne(blog.getArticleCategory().getId()));
		}
		return articleRepository.save(targetBlog);
	}

	@Transactional(readOnly = true)
	public Article findOne(long id) {
		return articleRepository.findOne(id);
	}

	@Transactional(readOnly = true)
	public Page<Article> findAll(int page) {
		Sort sort = new Sort(Sort.Direction.DESC, "id");
		PageRequest pageRequest = new PageRequest(page, PAGE_SIZE, sort);
		return articleRepository.findAll(pageRequest);
	}

	public void delete(long id) {
		articleRepository.delete(id);
	}
}
