package net.luversof.blog.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import net.luversof.blog.domain.BlogArticle;
import net.luversof.blog.domain.BlogArticleStatistics;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogArticleRepository;
import net.luversof.core.exception.BlueskyException;

@Service
public class BlogArticleService {

	private static final int PAGE_SIZE = 10;

	@Autowired
	private BlogArticleRepository articleRepository;
	
	@Autowired
	private BlogArticleCategoryService articleCategoryService;

	public BlogArticle save(BlogArticle article) {
		if (article.getArticleCategory() != null && article.getArticleCategory().getId() > 0) {
			article.setArticleCategory(articleCategoryService.findOne(article.getArticleCategory().getId()));
		}
		BlogArticleStatistics blogArticleStatistics = new BlogArticleStatistics();
		blogArticleStatistics.setBlogArticle(article);
		article.setBlogArticleStatistics(blogArticleStatistics);
		return articleRepository.save(article);
	}
	
	public BlogArticle update(BlogArticle article) {
		BlogArticle targetArticle = findOne(article.getId());
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

	public BlogArticle findOne(long id) {
		return articleRepository.findOne(id);
	}
	
	public void incraseViewCount(BlogArticle article) {
		BlogArticleStatistics articleStatistics = article.getBlogArticleStatistics();
		if (articleStatistics == null) {
			articleStatistics = new BlogArticleStatistics();
			articleStatistics.setBlogArticle(article);
			articleStatistics.setViewCount(1);
			article.setBlogArticleStatistics(articleStatistics);
		} else {
			article.getBlogArticleStatistics().setViewCount(article.getBlogArticleStatistics().getViewCount() + 1);
		}
		articleRepository.save(article);
	}

	public Page<BlogArticle> findByBlog(Blog blog, int page) {
		Sort sort = new Sort(Sort.Direction.DESC, "id");
		PageRequest pageRequest = new PageRequest(page, PAGE_SIZE, sort);
		return articleRepository.findByBlog(blog, pageRequest);
	}

	public void delete(long id) {
		articleRepository.delete(id);
	}
}
