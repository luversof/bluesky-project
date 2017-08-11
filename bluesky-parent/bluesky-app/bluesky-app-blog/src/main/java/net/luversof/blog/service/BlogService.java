package net.luversof.blog.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.core.exception.BlueskyException;
import net.luversof.core.exception.CoreErrorCode;
import net.luversof.user.service.LoginUserService;

/**
 * 블로그 관련 서비스
 * @author luver
 *
 */
@Service
public class BlogService {

	@Autowired
	private BlogRepository blogRepository;
	
	@Autowired
	private LoginUserService loginUserService;
	
	public Blog create() {
		UUID userId = loginUserService.getUserId().orElseThrow(() -> new BlueskyException(CoreErrorCode.NOT_EXIST_USER_ID));
		if (findByUserId(userId).isPresent()) {
			throw new BlueskyException(BlogErrorCode.ALREADY_EXIST_USER_BLOG);
		};
		
		Blog blog = new Blog();
		blog.setUserId(userId);
		return blogRepository.save(blog);
	}
	
	public Optional<Blog> findById(UUID id) {
		return blogRepository.findById(id);
	}

	public Optional<Blog> findByUserId(UUID userId) {
		return blogRepository.findByUserId(userId);
	}
}
