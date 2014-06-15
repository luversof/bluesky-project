package net.luversof.security;


import net.luversof.core.Banner;
import net.luversof.user.UserConfig;
import net.luversof.user.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.LuversofUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Import(value=UserConfig.class)
@ComponentScan
public class SecurityConfig {
	
	public SecurityConfig() {
		super();
		Banner.write(System.out, this.getClass().getPackage().getName());
	}

	@Autowired
	private UserService userService;
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	public UserDetailsService userDetailsService() {
		return new LuversofUserDetailsService(userService);
	}
}
