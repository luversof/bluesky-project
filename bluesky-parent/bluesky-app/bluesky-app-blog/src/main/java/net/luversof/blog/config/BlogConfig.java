package net.luversof.blog.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import net.luversof.blog.service.BlogService;
import net.luversof.blog.service.BlogUserService;
import net.luversof.blog.util.BlogRequestAttributeUtil;

@Configuration
public class BlogConfig {
	
	@Autowired
	private BlogService blogService;
	
	@Autowired
	private BlogUserService blogUserService;
	
	@PostConstruct
	public void postConstruct() {
		Assert.notNull(blogService);
		Assert.notNull(blogUserService);
		BlogRequestAttributeUtil.setBlogService(blogService, blogUserService);
	}
	

}
