package net.luversof.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.luversof.user.service.EmptyLoginUserService;
import net.luversof.user.service.LoginUserService;
import net.luversof.user.util.UserUtil;

@Configuration
public class UserConfig {

	@Bean
	@ConditionalOnMissingBean
	public LoginUserService loginUserService() {
		return new EmptyLoginUserService();
	}
	
	@Configuration
	public static class LoginUserServiceConfig {
		
		public LoginUserServiceConfig(LoginUserService loginUserService) {
			UserUtil.setLoginUserService(loginUserService);
		}
		
	}
}
