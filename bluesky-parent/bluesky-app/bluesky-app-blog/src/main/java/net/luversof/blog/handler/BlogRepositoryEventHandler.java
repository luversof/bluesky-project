package net.luversof.blog.handler;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.core.exception.BlueskyException;
import net.luversof.user.service.LoginUserService;

@Component
@RepositoryEventHandler
public class BlogRepositoryEventHandler {

	@Autowired
	private LoginUserService loginUserService;
	
	@Autowired
	private BlogRepository blogRepository;
	
	@HandleBeforeCreate
	public void handleBeforeCreate(Blog blog) {
		UUID userId = loginUserService.getUserId();
		if (blogRepository.findByUserId(userId).isPresent()) {
			throw new BlueskyException(BlogErrorCode.ALREADY_EXIST_USER_BLOG);
		};
		blog.setUserId(userId);	
	}
	
	@HandleBeforeSave
	public void handleBeforeSave(Blog blog) {
		UUID userId = loginUserService.getUserId();
		Blog userBlog = blogRepository.findByUserId(userId).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_USER_BLOG));
		if (!userBlog.getId().equals(blog.getId())) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOG);
		}
		blog.setUserId(userId);	
	}

}
