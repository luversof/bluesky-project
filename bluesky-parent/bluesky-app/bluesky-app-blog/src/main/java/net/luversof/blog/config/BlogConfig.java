package net.luversof.blog.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.util.Assert;

import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.blog.util.BlogRequestAttributeUtil;
import net.luversof.user.service.LoginUserService;

@Configuration
@PropertySource("classpath:blog.properties")
public class BlogConfig {
	
	@Autowired
	private BlogRepository blogRepository;
	
	@Autowired
	private LoginUserService loginUserService;
	
	@PostConstruct
	public void postConstruct() {
		Assert.notNull(blogRepository, "blogRepository must not be null");
		Assert.notNull(loginUserService, "loginUserService must not be null");
		BlogRequestAttributeUtil.setBlogRepository(blogRepository);
		BlogRequestAttributeUtil.setLoginUserService(loginUserService);
	}
	

	
	@Configuration
	public static class BlogRepositoryRestConfigurerAdapter implements RepositoryRestConfigurer {

		@Override
		public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
			config.exposeIdsFor(Blog.class, Article.class);
		}
		
	}
}
