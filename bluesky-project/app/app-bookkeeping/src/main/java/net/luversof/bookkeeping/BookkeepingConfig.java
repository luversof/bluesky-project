package net.luversof.bookkeeping;

import net.luversof.core.Banner;
import net.luversof.data.jpa.JpaConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(JpaConfig.class)
@ComponentScan
public class BookkeepingConfig {

	public BookkeepingConfig() {
		super();
		Banner.write(System.out, this.getClass().getPackage().getName());
	}

}