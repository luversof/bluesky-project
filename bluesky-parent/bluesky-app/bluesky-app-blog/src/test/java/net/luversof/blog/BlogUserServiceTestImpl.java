package net.luversof.blog;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import net.luversof.blog.service.BlogUserService;

@Service
public class BlogUserServiceTestImpl implements BlogUserService {

	@Override
	public Optional<UUID> getUserId() {
		return Optional.of(UUID.fromString("77a04682-3032-492c-9449-5ba986491eef"));
	}

}
