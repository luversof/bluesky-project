package net.luversof.blog.handler;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.blog.repository.CategoryRepository;
import net.luversof.boot.exception.BlueskyException;
import net.luversof.core.util.ValidationUtil;
import net.luversof.user.service.LoginUserService;

@Component
@RepositoryEventHandler
public class ArticleRepositoryEventHandler {
	
	@Autowired
	private LoginUserService loginUserService;
	
	@Autowired
	private BlogRepository blogRepository;
	
	@Autowired
	private CategoryRepository categoryRepository;
	
	@HandleBeforeCreate
	public void handleBeforeCreate(Article article) {
		ValidationUtil.validate(article, Article.Create.class);
		checkArticle(article);
	}
	
	
	@HandleBeforeSave
	public void handleBeforeSave(Article article) {
		ValidationUtil.validate(article, Article.Save.class);
		checkArticle(article);
	}
	
	@HandleBeforeDelete
	public void handleBeforeDelete(Article article) {
		checkArticle(article);
	}
	
	private void checkArticle(Article article) {
		UUID userId = loginUserService.getUserId();
		Blog blog = blogRepository.findByUserId(userId).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_USER_BLOG));
		if (article.getBlog() == null || !blog.getId().equals(article.getBlog().getId())) {
			throw new BlueskyException(BlogErrorCode.INVALID_PARAMETER_BLOG_ID);
		}
		
		if (article.getCategory() != null && article.getCategory().getId() > 0) {
			categoryRepository.findById(article.getCategory().getId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.INVALID_PARAMETER_CATEGORY_ID));
		}
	}
}
