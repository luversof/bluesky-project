package net.luversof.web.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

//@Configuration
//@EnableGlobalMethodSecurity(prePostEnabled = true)
//public class GlobalMethodSecurityConfig extends GlobalMethodSecurityConfiguration  {
//	
//	
//
//	@Override
//	protected AuthenticationManager authenticationManager() throws Exception {
//		return new AuthenticationManagerBuilder().build();
//	}
//	
//}
