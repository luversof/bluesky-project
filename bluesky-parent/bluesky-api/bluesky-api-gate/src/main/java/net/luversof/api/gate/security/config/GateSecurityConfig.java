package net.luversof.api.gate.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import net.luversof.api.gate.security.oauth2.client.service.GateOAuth2AuthorizedClientService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class GateSecurityConfig {
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@PostConstruct
	public void postConstruct() {
		objectMapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
	}

    @Bean
    @ConditionalOnMissingBean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    SecurityFilterChain gateSecurityFilterchain(HttpSecurity http, UserDetailsService userDetailsService, GateOAuth2AuthorizedClientService gateOAuth2AuthorizedClientService)
            throws Exception {

        http.userDetailsService(userDetailsService);
        http.authorizeHttpRequests((requests) -> requests.anyRequest().permitAll());
		http.oauth2Login(Customizer.withDefaults());
		http.oauth2Client().authorizedClientService(gateOAuth2AuthorizedClientService);

        var logoutSuccessHandler = new SimpleUrlLogoutSuccessHandler();
        logoutSuccessHandler.setUseReferer(true);

        var authenticationSuccessHandler = new SimpleUrlAuthenticationSuccessHandler();
        authenticationSuccessHandler.setUseReferer(true);

        http
                .headers().frameOptions().sameOrigin().and()
//                .exceptionHandling().authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)).and()
                .logout().logoutSuccessHandler(logoutSuccessHandler).and()
                .formLogin()
//                	.loginPage("/login")
                	.successHandler(authenticationSuccessHandler)
                .and()
                .rememberMe().and()
//                .httpBasic().and()
                .csrf().disable()
        ;

        return http.build();
    }

}
