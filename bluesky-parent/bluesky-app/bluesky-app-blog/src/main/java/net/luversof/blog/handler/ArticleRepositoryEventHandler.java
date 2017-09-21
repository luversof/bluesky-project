package net.luversof.blog.handler;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.blog.repository.CategoryRepository;
import net.luversof.core.exception.BlueskyException;
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
	public void HandleBeforeCreate(Article article) {
		UUID userId = loginUserService.getUserId();
		Blog blog = blogRepository.findByUserId(userId).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_USER_BLOG));
		if (!blog.getId().equals(article.getBlog().getId())) {
			throw new BlueskyException(BlogErrorCode.INVALID_PARAMETER_BLOG_ID);
		}
		
		if (article.getCategory() != null && article.getCategory().getId() > 0) {
			article.setCategory(categoryRepository.findById(article.getCategory().getId()).orElse(null));
		}
		article.setBlog(blog);
	}
}
