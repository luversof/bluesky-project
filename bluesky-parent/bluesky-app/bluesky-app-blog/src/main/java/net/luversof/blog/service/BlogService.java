package net.luversof.blog.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.boot.exception.BlueskyException;
import net.luversof.user.constant.UserErrorCode;
import net.luversof.user.util.UserUtil;

@Service
public class BlogService {
	
	@Autowired
	private BlogRepository blogRepository;
	
	/**
	 * 로그인한 유저 기준으로 조회
	 * 없으면 생성함
	 * @return
	 */
	public Optional<Blog> findByUserId() {
		UUID userId = UserUtil.getLoginUser().orElseThrow(() -> new BlueskyException(UserErrorCode.NEED_LOGIN)).getId();
		
		Optional<Blog> findBlog = findByUserId(userId);
		if (findBlog.isEmpty()) {
			return Optional.of(createBlog(userId));
		}
		return findBlog;

	}
	
	public Optional<Blog> findByUserId(UUID userId) {
		return blogRepository.findByUserId(userId);
	}

	public Blog createBlog(UUID userId) {
		Blog blog = new Blog();
		blog.setUserId(userId);
		return blogRepository.save(blog);
	}
}
