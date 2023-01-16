package net.luversof.user.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;

@Configuration
public class UserConfig {

	@Bean
	UserDetailsManager userDetailsManager(@Qualifier("userDataSource") DataSource userDataSource) {
		return new JdbcUserDetailsManager(userDataSource);
	}

}
