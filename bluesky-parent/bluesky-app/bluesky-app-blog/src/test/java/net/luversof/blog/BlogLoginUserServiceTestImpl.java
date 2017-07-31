package net.luversof.blog;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import net.luversof.user.service.LoginUserService;

@Service
public class BlogLoginUserServiceTestImpl implements LoginUserService {

	@Override
	public Optional<UUID> getUserId() {
		return Optional.of(UUID.fromString("77a04682-3032-492c-9449-5ba986491eef"));
	}

}
