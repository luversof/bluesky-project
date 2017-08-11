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
		return Optional.of(UUID.fromString("77a04682-3032-492c-9449-5ba986491eef"));
	}

	@Override
	public Optional<User> getUser() {
		return Optional.empty();
	}

}
