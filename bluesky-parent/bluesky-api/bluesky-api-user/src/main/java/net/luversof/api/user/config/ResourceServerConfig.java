package net.luversof.api.user.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class ResourceServerConfig {

  /** Header the bluesky-client-user callers send; must match bluesky.internal-api.token. */
  public static final String INTERNAL_TOKEN_HEADER = "X-Bluesky-Internal-Token";

  @Bean
  @Order(2)
  SecurityFilterChain resourceServerSecurityFilterChain(
      HttpSecurity http, @Value("${bluesky.internal-api.token:}") String internalToken)
      throws Exception {
    // These paths read and write any user's session and create users, and the callers (gates) hold
    // no JWT. Without this check anyone reaching api-user could forge a logged-in session.
    http.securityMatcher("/api/**")
        .authorizeHttpRequests(authorize ->
            authorize
                .requestMatchers(
                    "/api/oAuth2AuthorizedClient/**",
                    "/api/userInfo/oauth2",
                    "/api/userInfo/search/findByProvider",
                    "/api/userInfo/search/findByIdIn",
                    "/api/userInfo/create-new-session",
                    "/api/userInfo/create-session",
                    "/api/userInfo/validate-session",
                    "/api/userInfo/delete-session")
                .access((authentication, context) ->
                    new AuthorizationDecision(
                        isInternalCall(
                            internalToken, context.getRequest().getHeader(INTERNAL_TOKEN_HEADER))))
                .anyRequest()
                .authenticated())
        .oauth2ResourceServer(oauth2 ->
            oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .csrf(csrf -> csrf.disable());

    return http.build();
  }

  /** Fails closed: an unset expected token rejects every call. Constant-time compare. */
  static boolean isInternalCall(String expected, String actual) {
    if (expected == null || expected.isBlank() || actual == null) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
        new JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthoritiesClaimName("scope");
    grantedAuthoritiesConverter.setAuthorityPrefix("SCOPE_");

    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

    return jwtAuthenticationConverter;
  }
}
