package net.luversof.blog.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.domain.BlogArticle;
import net.luversof.blog.domain.BlogArticleStatistics;
import net.luversof.blog.repository.BlogArticleRepository;
import net.luversof.core.exception.BlueskyException;

@Service
public class BlogArticleService {

	@Autowired
	private BlogArticleRepository articleRepository;
	
	@Autowired
	private BlogArticleCategoryService articleCategoryService;
	
	public BlogArticle save(BlogArticle blogArticle) {
		if (blogArticle.getBlogArticleCategory() != null && blogArticle.getBlogArticleCategory().getId() > 0) {
			blogArticle.setBlogArticleCategory(articleCategoryService.findOne(blogArticle.getBlogArticleCategory().getId()));
		}
		BlogArticleStatistics blogArticleStatistics = new BlogArticleStatistics();
		blogArticleStatistics.setBlogArticle(blogArticle);
		blogArticle.setBlogArticleStatistics(blogArticleStatistics);
		return articleRepository.save(blogArticle);
	}
	
	public BlogArticle update(BlogArticle blogArticle) {
		BlogArticle targetBlogArticle = findOne(blogArticle.getId());
		if (!blogArticle.getBlog().equals(targetBlogArticle.getBlog())) {
			throw new BlueskyException("blog.article.permissionDenied");
		}
		targetBlogArticle.setTitle(blogArticle.getTitle());
		targetBlogArticle.setContent(blogArticle.getContent());
		if (blogArticle.getBlogArticleCategory() != null && blogArticle.getBlogArticleCategory().getId() != 0) {
			targetBlogArticle.setBlogArticleCategory(articleCategoryService.findOne(blogArticle.getBlogArticleCategory().getId()));
		}
		return articleRepository.save(targetBlogArticle);
	}

	public BlogArticle findOne(long id) {
		return articleRepository.getOne(id);
	}
	
	public void incraseViewCount(BlogArticle blogArticle) {
		BlogArticleStatistics blogArticleStatistics = blogArticle.getBlogArticleStatistics();
		if (blogArticleStatistics == null) {
			blogArticleStatistics = new BlogArticleStatistics();
			blogArticleStatistics.setBlogArticle(blogArticle);
			blogArticleStatistics.setViewCount(1);
			blogArticle.setBlogArticleStatistics(blogArticleStatistics);
		} else {
			blogArticle.getBlogArticleStatistics().setViewCount(blogArticle.getBlogArticleStatistics().getViewCount() + 1);
		}
		articleRepository.save(blogArticle);
	}

	public Page<BlogArticle> findByBlog(Blog blog, Pageable pageable) {
		return articleRepository.findByBlog(blog, pageable);
	}

	public void delete(long id) {
		articleRepository.deleteById(id);
	}
}
