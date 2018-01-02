package net.luversof.user.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.luversof.user.service.EmptyLoginUserService;
import net.luversof.user.service.LoginUserService;
import net.luversof.user.util.UserUtil;

@Configuration
public class UserConfig {
	
	@Autowired
	private LoginUserService loginUserService;
	
	@PostConstruct
	public void postConstruct() {
		UserUtil.setLoginUserService(loginUserService);
	}

	@Bean
	@ConditionalOnMissingBean
	public LoginUserService loginUserService() {
		return new EmptyLoginUserService();
	}
}
