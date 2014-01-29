package net.luversof.security.config;

import lombok.SneakyThrows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

@Configuration
@EnableWebMvcSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private UserDetailsService userDetailsService;
	
	 @Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		 auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
	}

	@Override
	public void configure(WebSecurity web) {
		web.ignoring().antMatchers("/resources/**", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/loginPage");
	}

	@Override
	@SneakyThrows
	protected void configure(HttpSecurity http) {
		SimpleUrlLogoutSuccessHandler logoutSuccessHandler = new SimpleUrlLogoutSuccessHandler();
		logoutSuccessHandler.setUseReferer(true);
		http
			.authorizeRequests()
				.antMatchers("/test/**").permitAll()
//				.anyRequest().authenticated()
			.and()
			.logout().logoutSuccessHandler(logoutSuccessHandler).and()
			.formLogin().loginPage("/login").and()
			.rememberMe().and()
            .httpBasic().and()
            .csrf().and();
	}
}
