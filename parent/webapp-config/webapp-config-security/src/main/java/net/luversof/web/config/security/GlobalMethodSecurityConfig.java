package net.luversof.web.config.security;

import javax.sql.DataSource;

import lombok.SneakyThrows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class GlobalMethodSecurityConfig extends GlobalMethodSecurityConfiguration  {
	@Autowired
	private DataSource securityDataSource;
	
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	@SneakyThrows
	protected void registerAuthentication(AuthenticationManagerBuilder auth) {
		auth.jdbcAuthentication().dataSource(securityDataSource).passwordEncoder(passwordEncoder);
	}
}
