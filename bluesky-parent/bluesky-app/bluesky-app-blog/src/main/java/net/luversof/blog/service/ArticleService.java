package net.luversof.blog.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.Article;
import net.luversof.blog.repository.ArticleRepository;
import net.luversof.core.exception.BlueskyException;
import net.luversof.core.exception.CoreErrorCode;
import net.luversof.user.service.LoginUserService;

@Service
public class ArticleService {

	@Autowired
	private ArticleRepository articleRepository;
	
	@Autowired
	private LoginUserService loginUserService;
	
	@Autowired
	private BlogService blogService;
	
	@Autowired
	private CategoryService categoryService;
	
	public Article save(Article article) {
		if (article.getCategory() != null && article.getCategory().getId() > 0) {
			article.setCategory(categoryService.findById(article.getCategory().getId()).orElse(null));
		}
		return articleRepository.save(article);
	}
	
	public Article update(Article article) {
		UUID userId = loginUserService.getUserId().orElseThrow(() -> new BlueskyException(CoreErrorCode.NOT_EXIST_USER_ID));
		Blog blog = blogService.findByUserId(userId).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOG));
		
		if (!blog.getId().equals(article.getBlog().getId())) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOG);
		}
		
		Article targetBlogArticle = findById(article.getId()).get();
		if (!article.getBlog().getId().equals(targetBlogArticle.getBlog().getId())) {
			throw new BlueskyException(BlogErrorCode.INVALID_PARAMETER_ARTICLE_ID);
		}
		targetBlogArticle.setTitle(article.getTitle());
		targetBlogArticle.setContent(article.getContent());
		if (article.getCategory() != null && article.getCategory().getId() != 0) {
			targetBlogArticle.setCategory(categoryService.findById(article.getCategory().getId()).orElse(null));
		}
		return articleRepository.save(targetBlogArticle);
	}

	public Optional<Article> findById(long id) {
		return articleRepository.findById(id);
	}
	
	public void incraseViewCount(Article article) {
		article.setViewCount(article.getViewCount() + 1);
		articleRepository.save(article);
	}

	public Page<Article> findByBlog(Blog blog, Pageable pageable) {
		return articleRepository.findByBlog(blog, pageable);
	}

	public void delete(long id) {
		articleRepository.deleteById(id);
	}
}
