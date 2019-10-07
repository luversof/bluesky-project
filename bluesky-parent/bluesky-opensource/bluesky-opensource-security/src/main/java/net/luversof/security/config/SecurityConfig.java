package net.luversof.security.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import net.luversof.security.service.SecurityLoginUserService;
import net.luversof.security.util.SecurityUtil;

@Configuration
@PropertySource("classpath:security.properties")
public class SecurityConfig {
	
	@Autowired
	private SecurityLoginUserService securityLoginUserService;
	
	@PostConstruct
	public void postConstruct() {
		SecurityUtil.setSecurityLoginUserService(securityLoginUserService);
	}

}
