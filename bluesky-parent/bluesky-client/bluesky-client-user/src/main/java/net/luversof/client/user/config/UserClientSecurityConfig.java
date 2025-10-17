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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import io.github.luversof.boot.context.ApplicationContextUtil;
import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.client.user.constant.ClientUserErrorCode;
import net.luversof.client.user.openfeign.OAuth2AuthorizedClientClient;
import net.luversof.client.user.openfeign.OAuth2AuthorizedClientClient.SaveAuthorizedClientParam;
import net.luversof.client.user.openfeign.UserDetailsClient;

@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity
@PropertySource(value = "classpath:client/user.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:client/user-${bluesky-boot-profile}.properties", ignoreResourceNotFound = true)
public class UserClientSecurityConfig {

	@Bean
	@ConditionalOnMissingBean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	UserDetailsManager userClientUserDetailsManager(UserDetailsClient userDetailsClient) {
		return new UserDetailsManager() {
			@Override
			public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
				return userDetailsClient.loadUserByUsername(username).orElseThrow(() -> new BlueskyException(ClientUserErrorCode.NOT_EXIST_USER));
			}

			@Override
			public void createUser(UserDetails user) {
				userDetailsClient.createUser((User) user);
			}

			@Override
			public void updateUser(UserDetails user) {
				userDetailsClient.updateUser((User) user);
			}

			@Override
			public void deleteUser(String username) {
				userDetailsClient.deleteUser(username);
			}

			@Override
			public void changePassword(String oldPassword, String newPassword) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public boolean userExists(String username) {
				// TODO Auto-generated method stub
				return false;
			}
		};
	}

	@Bean
	OAuth2AuthorizedClientService userClientUserOAuth2AuthorizedClientService() {
		return new OAuth2AuthorizedClientService() {
			@SuppressWarnings("unchecked")
			@Override
			public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
				return (T) getClient().loadAuthorizedClient(clientRegistrationId, principalName);
			}

			@Override
			public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
				getClient().saveAuthorizedClient(new SaveAuthorizedClientParam(authorizedClient, principal));
			}

			@Override
			public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
				getClient().removeAuthorizedClient(clientRegistrationId, principalName);
			}
			
			private OAuth2AuthorizedClientClient getClient() {
				return ApplicationContextUtil.getApplicationContext().getBean(OAuth2AuthorizedClientClient.class);
			}
		};
	}

	@Bean
	SecurityFilterChain gateSecurityFilterchain(
			HttpSecurity http, 
			UserDetailsService userDetailsService, 
			OAuth2AuthorizedClientService clientUserOAuth2AuthorizedClientService
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
