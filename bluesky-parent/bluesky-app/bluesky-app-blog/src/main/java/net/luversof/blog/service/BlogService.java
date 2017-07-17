package net.luversof.blog.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.exception.BlogErrorCode;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.core.exception.BlueskyException;

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
	private BlogUserService blogUserService;
	
	public Blog create() {
		String userId = blogUserService.getUserId().orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_USER_ID));
		if (!findByUserId(userId).isEmpty()) {
			throw new BlueskyException(BlogErrorCode.ALREADY_EXIST_BLOG);
		};
		Blog blog = new Blog();
		blog.setUserId(userId);
		return blogRepository.save(blog);
	}
	
	public Optional<Blog> findById(UUID id) {
		return blogRepository.findById(id);
	}

	public List<Blog> findByUserId(String userId) {
		return blogRepository.findByUserId(userId);
	}
}
