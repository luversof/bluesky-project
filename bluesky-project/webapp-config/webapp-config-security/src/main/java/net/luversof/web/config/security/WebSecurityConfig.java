package net.luversof.web.config.security;

import lombok.SneakyThrows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private UserDetailsService userDetailsService;
	
	@Override
	@SneakyThrows
	protected void registerAuthentication(AuthenticationManagerBuilder auth) {
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
			.authorizeUrls()
				.antMatchers("/test/**").permitAll()
//				.anyRequest().authenticated()
			.and()
			.logout().logoutSuccessHandler(logoutSuccessHandler).and()
			.formLogin().loginPage("/loginPage").and()
			.rememberMe().and()
            .httpBasic();
	}
}
