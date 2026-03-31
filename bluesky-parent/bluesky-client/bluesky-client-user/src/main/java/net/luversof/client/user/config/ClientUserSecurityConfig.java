package net.luversof.client.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
@EnableWebSecurity
public class ClientUserSecurityConfig {

    @Bean
    ClientUserProperties clientUserProperties() {
        return new ClientUserProperties();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain clientUserSecurityFilterChain(
            HttpSecurity http, ClientUserProperties clientUserProperties) throws Exception {
        return http.csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .exceptionHandling(
                        exception ->
                                exception.authenticationEntryPoint(
                                        (request, response, authException) -> {
                                            String url =
                                                    UriComponentsBuilder.fromUriString(
                                                                    clientUserProperties
                                                                            .getLoginUrl())
                                                            .queryParam(
                                                                    "redirectUrl",
                                                                    ServletUriComponentsBuilder
                                                                            .fromRequest(request)
                                                                            .build()
                                                                            .toUriString())
                                                            .build()
                                                            .toUriString();
                                            response.sendRedirect(url);
                                        }))
                .oauth2Client(Customizer.withDefaults())
                .logout(
                        logout ->
                                logout.logoutUrl("/logout")
                                        .logoutSuccessUrl("/")
                                        .invalidateHttpSession(true)
                                        .clearAuthentication(true))
                .build();
    }
}
