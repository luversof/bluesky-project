package net.luversof.user.service;

import java.util.Optional;
import java.util.UUID;

import net.luversof.user.domain.User;

/**
 * 실제 연동되는 loginUserService가 없을 경우 사용되는 서비스
 * @author bluesky
 *
 */
public class EmptyLoginUserService implements LoginUserService {

	@Override
	public Optional<User> getUser() {
		return Optional.empty();
	}

	@Override
	public UUID getUserId() {
		return UUID.randomUUID();
	}

}
