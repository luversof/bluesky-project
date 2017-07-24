package net.luversof.blog.service;

import java.util.Optional;
import java.util.UUID;

public interface BlogUserService {
	/**
	 * 로그인한 유저의 UserId를 반환
	 * @return
	 */
	Optional<UUID> getUserId();
}
