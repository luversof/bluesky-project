package net.luversof.user;

import net.luversof.core.Banner;
import net.luversof.core.BlueskyCoreConfig;
import net.luversof.data.jpa.DataJpaConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComponentScan
@Configuration
@Import({ BlueskyCoreConfig.class, DataJpaConfig.class })
public class UserConfig {

	public UserConfig() {
		Banner.write(this);
	}

}
