package net.luversof.user;

import net.luversof.core.Banner;
import net.luversof.data.jpa.JpaConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(JpaConfig.class)
@ComponentScan
public class UserConfig {

	public UserConfig() {
		super();
		Banner.write(System.out, this.getClass().getPackage().getName());
	}
	
}
