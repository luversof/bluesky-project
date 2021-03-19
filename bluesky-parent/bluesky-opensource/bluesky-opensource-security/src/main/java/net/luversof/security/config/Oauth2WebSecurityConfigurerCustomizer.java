package net.luversof.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import io.github.luversof.boot.autoconfigure.security.servlet.WebSecurityConfigurerCustomizer;
import lombok.SneakyThrows;
import net.luversof.security.oauth2.client.BlueskyOAuth2AuthorizedClientService;

@Component
public class Oauth2WebSecurityConfigurerCustomizer implements WebSecurityConfigurerCustomizer {
	
	@Autowired
	private BlueskyOAuth2AuthorizedClientService blueskyOAuth2AuthorizedClientService;

	@SneakyThrows
	@Override
	public void postConfigure(HttpSecurity http) {
		SimpleUrlAuthenticationSuccessHandler authenticationSuccessHandler = new SimpleUrlAuthenticationSuccessHandler();
		authenticationSuccessHandler.setUseReferer(true);
		
		http.oauth2Login().successHandler(authenticationSuccessHandler).authorizedClientService(blueskyOAuth2AuthorizedClientService);
	}

	
}
