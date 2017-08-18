package net.luversof.bookkeeping;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import net.luversof.user.domain.User;
import net.luversof.user.service.LoginUserService;

@Service
public class BookkeepingUserServiceTestImpl implements LoginUserService {

	@Override
	public Optional<UUID> getUserId() {
		return Optional.of(UUID.randomUUID());
	}

	@Override
	public Optional<User> getUser() {
		return Optional.empty();
	}

}
