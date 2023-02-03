package net.luversof.web.gate.user.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import net.luversof.web.gate.user.service.GateOAuth2AuthorizedClientService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class GateSecurityConfig {
	
    @Bean
    @ConditionalOnMissingBean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
    
    @Bean
    GrantedAuthoritiesMapper userAuthoritiesMapper() {
    	return authorities -> {
    		if (!(authorities instanceof OAuth2UserAuthority)) {
    			return authorities;
    		}
    		
    		Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
    		mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    		mappedAuthorities.addAll(authorities);
    		return mappedAuthorities;
    	};
    }

    @Bean
    SecurityFilterChain gateSecurityFilterchain(HttpSecurity http, UserDetailsService userDetailsService, GateOAuth2AuthorizedClientService gateOAuth2AuthorizedClientService)
            throws Exception {

        http.userDetailsService(userDetailsService);
        http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
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
