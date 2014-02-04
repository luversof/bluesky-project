package net.luversof.security.config;

//import lombok.SneakyThrows;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
//import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//@Configuration
//@EnableGlobalMethodSecurity(prePostEnabled = true)
//public class GlobalMethodSecurityConfig extends GlobalMethodSecurityConfiguration  {
//	@Autowired
//	private PasswordEncoder passwordEncoder;
//	
//	@Autowired
//	private UserDetailsService userDetailsService;
//	
//
//	@Override
//	@SneakyThrows
//	protected void registerAuthentication(AuthenticationManagerBuilder auth) {
//		auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
//	}
//}
