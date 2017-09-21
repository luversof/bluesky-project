package net.luversof.user.service;

import java.util.Optional;
import java.util.UUID;

import net.luversof.user.domain.User;

public interface LoginUserService {
	
	Optional<User> getUser();
	
	/**
	 * 로그인한 유저의 UserId를 반환
	 * @return
	 */
	UUID getUserId();
}
