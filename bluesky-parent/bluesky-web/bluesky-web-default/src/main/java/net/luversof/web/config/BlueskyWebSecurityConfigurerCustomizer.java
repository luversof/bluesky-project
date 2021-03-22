package net.luversof.web.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

import io.github.luversof.boot.autoconfigure.security.servlet.WebSecurityConfigurerCustomizer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BlueskyWebSecurityConfigurerCustomizer implements WebSecurityConfigurerCustomizer {

	@Override
	public void preConfigure(HttpSecurity http) {
		try {
			http.csrf().disable();
		} catch (Exception e) {
			log.error("preConfigure error : ", e);
		}
	}

	
}
