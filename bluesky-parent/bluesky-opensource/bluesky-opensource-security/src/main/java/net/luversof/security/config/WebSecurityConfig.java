package net.luversof.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.web.filter.RequestContextFilter;

import lombok.SneakyThrows;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private UserDetailsService userDetailsService;
	
	@Autowired
	private OAuth2ClientContextFilter oAuth2ClientContextFilter;
	
	@Autowired
	private OAuth2ClientAuthenticationProcessingFilter githubAuthenticationProcessingFilter;
	
	@Autowired
	private OAuth2ClientAuthenticationProcessingFilter facebookAuthenticationProcessingFilter;
	
	@Autowired
	private OAuth2ClientAuthenticationProcessingFilter battleNetAuthenticationProcessingFilter;
	
//	@Autowired
//	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
//		auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
//	}
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
		super.configure(auth);
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
//			.addFilterBefore(new OAuth2ClientContextFilter(), UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(new RequestContextFilter(), SecurityContextPersistenceFilter.class)
			.addFilterAfter(oAuth2ClientContextFilter, BasicAuthenticationFilter.class)
			.addFilterAfter(githubAuthenticationProcessingFilter, BasicAuthenticationFilter.class)
			.addFilterAfter(facebookAuthenticationProcessingFilter, BasicAuthenticationFilter.class)
			.addFilterAfter(battleNetAuthenticationProcessingFilter, BasicAuthenticationFilter.class)
//			.addFilterAfter(oAuth2AuthenticationProcessingFilter, ExceptionTranslationFilter.class)
			.exceptionHandling().accessDeniedPage("/error/accessDenied").and()
			.logout().logoutSuccessHandler(logoutSuccessHandler).and()
			.formLogin().loginPage("/login").and()
			.rememberMe().and()
//			.csrf().and()
            .httpBasic().and()
            ;
	}
}
