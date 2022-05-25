package net.luversof.blog.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.repository.mysql.BlogRepository;
import net.luversof.user.constant.UserErrorCode;
import net.luversof.user.util.UserUtil;

@Service
public class BlogService {
	
	@Autowired
	private BlogRepository blogRepository;
	
	/**
	 * 로그인한 유저의 blog 조회
	 * 유저당 blog가 1개인 전제로 사용하는 호출
	 * @return
	 */
	public Optional<Blog> findByUserId() {
		var userId = UserUtil.getLoginUser().orElseThrow(() -> new BlueskyException(UserErrorCode.NEED_LOGIN)).getUserId();
		var findBlog = findByUserId(userId);
		if (findBlog.isEmpty()) {
			return Optional.of(createBlog(userId));
		}
		return findBlog;

	}
	
	
	
	public Optional<Blog> findByUserId(String userId) {
		return blogRepository.findByUserId(userId);
	}

	public Blog createBlog(String userId) {
		var blog = new Blog();
		blog.setUserId(userId);
		return blogRepository.save(blog);
	}
}
