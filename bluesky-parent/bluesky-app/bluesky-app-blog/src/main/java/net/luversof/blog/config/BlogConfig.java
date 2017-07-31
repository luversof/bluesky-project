package net.luversof.blog.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.Assert;

import net.luversof.blog.service.BlogService;
import net.luversof.blog.util.BlogRequestAttributeUtil;
import net.luversof.user.service.LoginUserService;

@Configuration
@PropertySource("classpath:blog.properties")
public class BlogConfig {
	
	@Autowired
	private BlogService blogService;
	
	@Autowired
	private LoginUserService loginUserService;
	
	@PostConstruct
	public void postConstruct() {
		Assert.notNull(blogService, "blogService must not be null");
		Assert.notNull(loginUserService, "loginUserService must not be null");
		BlogRequestAttributeUtil.setBlogService(blogService);
		BlogRequestAttributeUtil.setLoginUserService(loginUserService);
	}
	

}
