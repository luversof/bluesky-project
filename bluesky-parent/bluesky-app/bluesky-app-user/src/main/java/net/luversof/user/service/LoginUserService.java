package net.luversof.user.service;

import java.util.Optional;

import net.luversof.user.domain.User;

public interface LoginUserService {
	
	Optional<User> getUser();
	
	/**
	 * 로그인한 유저의 UserId를 반환
	 * @return
	 */
	String getUserId();
}
