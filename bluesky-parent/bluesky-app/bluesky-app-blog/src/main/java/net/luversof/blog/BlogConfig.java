package net.luversof.blog;

import net.luversof.core.Banner;
import net.luversof.core.BlueskyCoreConfig;
import net.luversof.data.jpa.JpaConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ BlueskyCoreConfig.class, JpaConfig.class })
@ComponentScan
public class BlogConfig {

	public BlogConfig() {
		Banner.write(this);
	}

}
