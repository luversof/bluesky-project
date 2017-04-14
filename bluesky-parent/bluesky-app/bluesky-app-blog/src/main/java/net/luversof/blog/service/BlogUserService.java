package net.luversof.blog.service;

import java.util.Optional;

public interface BlogUserService {
	/**
	 * 로그인한 유저의 UserId를 반환
	 * @return
	 */
	Optional<String> getUserId();
}
