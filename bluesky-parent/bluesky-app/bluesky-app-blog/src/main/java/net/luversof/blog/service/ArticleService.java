package net.luversof.blog.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.ArticleStatistics;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.ArticleRepository;
import net.luversof.core.exception.BlueskyException;

@Service
@Transactional("blogTransactionManager")
public class ArticleService {

	private static final int PAGE_SIZE = 10;

	@Autowired
	private ArticleRepository articleRepository;
	
	@Autowired
	private ArticleCategoryService articleCategoryService;

	public Article save(Article article) {
		if (article.getArticleCategory() != null && article.getArticleCategory().getId() > 0) {
			article.setArticleCategory(articleCategoryService.findOne(article.getArticleCategory().getId()));
		}
		ArticleStatistics articleStatistics = new ArticleStatistics();
		articleStatistics.setArticle(article);
		article.setArticleStatistics(articleStatistics);
		return articleRepository.save(article);
	}
	
	public Article update(Article article) {
		Article targetArticle = findOne(article.getId());
		if (!article.getBlog().equals(targetArticle.getBlog())) {
			throw new BlueskyException("blog.article.permissionDenied");
		}
		targetArticle.setTitle(article.getTitle());
		targetArticle.setContent(article.getContent());
		if (article.getArticleCategory() != null && article.getArticleCategory().getId() != 0) {
			targetArticle.setArticleCategory(articleCategoryService.findOne(article.getArticleCategory().getId()));
		}
		return articleRepository.save(targetArticle);
	}

	@Transactional(value = "blogTransactionManager", readOnly = true)
	public Article findOne(long id) {
		return articleRepository.findOne(id);
	}
	
	public void incraseViewCount(Article article) {
		ArticleStatistics articleStatistics = article.getArticleStatistics();
		if (articleStatistics == null) {
			articleStatistics = new ArticleStatistics();
			articleStatistics.setArticle(article);
			articleStatistics.setViewCount(1);
			article.setArticleStatistics(articleStatistics);
		} else {
			article.getArticleStatistics().setViewCount(article.getArticleStatistics().getViewCount() + 1);
		}
		articleRepository.save(article);
	}

	@Transactional(value = "blogTransactionManager", readOnly = true)
	public Page<Article> findByBlog(Blog blog, int page) {
		Sort sort = new Sort(Sort.Direction.DESC, "id");
		PageRequest pageRequest = new PageRequest(page, PAGE_SIZE, sort);
		return articleRepository.findByBlog(blog, pageRequest);
	}

	public void delete(long id) {
		articleRepository.delete(id);
	}
}
