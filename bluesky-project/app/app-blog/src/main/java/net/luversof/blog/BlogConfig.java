package net.luversof.blog;

import net.luversof.core.Banner;
import net.luversof.data.jpa.JpaConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(JpaConfig.class)
@ComponentScan
public class BlogConfig {

	public BlogConfig() {
		super();
		Banner.write(System.out, "app-blog");
	}
	
}
