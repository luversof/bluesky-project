package net.luversof.security.config;

import lombok.SneakyThrows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
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
	 
	 
	/* (s) ldap 관련 설정 */
	@Autowired
	private LdapAuthenticationProvider ldapAuthenticationProvider;
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(ldapAuthenticationProvider);
		//.ldapAuthentication()
//		.userDnPatterns("uid={0},ou=people")
//		.groupSearchBase("ou=groups")
		//.contextSource()
//			.ldif("test-server.ldif");
	}
	/* (e) ldap 관련 설정 */

	
	


	@Override
	public void configure(WebSecurity web) {
		web.ignoring().antMatchers("/resources/**", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/loginPage");
	}

	@Override
	@SneakyThrows
	protected void configure(HttpSecurity http) {
		SimpleUrlLogoutSuccessHandler logoutSuccessHandler = new SimpleUrlLogoutSuccessHandler();
		logoutSuccessHandler.setUseReferer(true);
		/* (s) ldap 관련 설정 */
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		/* (e) ldap 관련 설정 */
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
