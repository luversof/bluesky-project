package net.luversof.blog;

import java.util.Optional;

import org.springframework.stereotype.Service;

import net.luversof.blog.service.BlogUserService;

@Service
public class BlogUserServiceTestImpl implements BlogUserService {

	@Override
	public Optional<String> getUserId() {
		return Optional.of("TEST");
	}

}
