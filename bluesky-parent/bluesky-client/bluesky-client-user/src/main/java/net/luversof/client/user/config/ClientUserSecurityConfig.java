package net.luversof.client.user.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import net.luversof.client.user.openfeign.client.UserDetailsClient;
import net.luversof.client.user.provisioning.ClientUserDetailsManager;
import net.luversof.client.user.service.ClientUserOAuth2AuthorizedClientService;

@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity
@PropertySource(value = "classpath:client/user.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:client/user-${bluesky-boot-profile}.properties", ignoreResourceNotFound = true)
public class ClientUserSecurityConfig {
	
    @Bean
    @ConditionalOnMissingBean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
    
    @Bean
    UserDetailsManager clientUserDetailsManager(UserDetailsClient userDetailsClient) {
    	return new ClientUserDetailsManager(userDetailsClient);
    }
    
    @Bean
    OAuth2AuthorizedClientService clientUserOAuth2AuthorizedClientService() {
    	return new ClientUserOAuth2AuthorizedClientService();
    }
    
    @Bean
    SecurityFilterChain gateSecurityFilterchain(
    		HttpSecurity http, 
    		UserDetailsService userDetailsService, 
    		ClientUserOAuth2AuthorizedClientService clientUserOAuth2AuthorizedClientService
    		) throws Exception {
    	var logoutSuccessHandler = new SimpleUrlLogoutSuccessHandler();
    	logoutSuccessHandler.setUseReferer(true);
    	
    	var authenticationSuccessHandler = new SavedRequestAwareAuthenticationSuccessHandler();
    	authenticationSuccessHandler.setUseReferer(true);
    	authenticationSuccessHandler.setTargetUrlParameter("targetUrl");

        http
        	.userDetailsService(userDetailsService)
        	.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
        	.oauth2Login(oauth2 -> oauth2
        			.permitAll()
        			.successHandler(authenticationSuccessHandler)
        			.userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(userAuthoritiesMapper()))
        	)
        	.oauth2Client(config -> config.authorizedClientService(clientUserOAuth2AuthorizedClientService))
            .headers(config -> config.frameOptions(FrameOptionsConfig::sameOrigin))
            .logout(config -> config.logoutSuccessHandler(logoutSuccessHandler))
            .formLogin(config -> config
            		.permitAll()
            		.successHandler(authenticationSuccessHandler))
            
            .csrf(CsrfConfigurer::disable)
        ;

        return http.build();
    }
    
    private GrantedAuthoritiesMapper userAuthoritiesMapper() {
    	return (authorities) -> {
    		Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
    		
    		authorities.forEach(authority -> {
				if ((authority instanceof OAuth2UserAuthority) && mappedAuthorities.isEmpty()) {
					mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
				}
			});
    		
    		mappedAuthorities.addAll(authorities);
    		return mappedAuthorities;
    	};
    }

}
