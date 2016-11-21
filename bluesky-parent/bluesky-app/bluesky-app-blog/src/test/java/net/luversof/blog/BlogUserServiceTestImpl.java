package net.luversof.blog;

import org.springframework.stereotype.Service;

import net.luversof.blog.service.BlogUserService;

@Service
public class BlogUserServiceTestImpl implements BlogUserService {

	@Override
	public String getUserId() {
		return "TEST";
	}

}
