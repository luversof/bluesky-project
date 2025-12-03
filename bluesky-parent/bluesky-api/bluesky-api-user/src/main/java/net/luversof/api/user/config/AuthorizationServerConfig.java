package net.luversof.api.user.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class AuthorizationServerConfig {
	
	@Bean
	@Order(1)
	SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
		var authorizationServerConfigurer =	new OAuth2AuthorizationServerConfigurer();

		return http
				.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
				.with(authorizationServerConfigurer, configurer -> 
					configurer.oidc(Customizer.withDefaults()))
				.authorizeHttpRequests(authorize -> 
					authorize.anyRequest().authenticated())
				.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
				.build();
	}

	@Value("${bluesky.oauth2.registered-client.web-gate.redirect-uris}")
	private String redirectUris;

	@Value("${bluesky.oauth2.registered-client.web-gate.post-logout-redirect-uris}")
	private String postLogoutRedirectUris;

	@Bean
	RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
		JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);

		// bluesky-web-gate 클라이언트 등록 (이미 존재하면 skip)
		try {
			RegisteredClient existingClient = repository.findByClientId("bluesky-web-gate");
			if (existingClient == null) {
				var builder = RegisteredClient.withId(UUID.randomUUID().toString())
						.clientId("bluesky-web-gate")
						.clientSecret("{noop}secret")
						.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
						.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
						.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
						.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
						.authorizationGrantType(
								new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:token-exchange"));

				// Redirect URIs from properties
				Arrays.stream(redirectUris.split(","))
						.map(String::trim)
						.forEach(builder::redirectUri);

				// Post Logout Redirect URIs from properties
				Arrays.stream(postLogoutRedirectUris.split(","))
						.map(String::trim)
						.forEach(builder::postLogoutRedirectUri);

				RegisteredClient webGateClient = builder
						.scope(OidcScopes.OPENID)
						.scope(OidcScopes.PROFILE)
						.scope(OidcScopes.EMAIL)
						.scope("board.read")
						.scope("board.write")
						.scope("stock.read")
						.scope("stock.write")
						.clientSettings(ClientSettings.builder()
								.requireAuthorizationConsent(false)
								.build())
						.tokenSettings(TokenSettings.builder()
								.accessTokenTimeToLive(Duration.ofHours(2))
								.refreshTokenTimeToLive(Duration.ofDays(30))
								.reuseRefreshTokens(false)
								.build())
						.build();

				repository.save(webGateClient);
			}
		} catch (Exception e) {
			// 테이블이 없거나 다른 오류 발생 시 무시 (초기 실행 시)
		}

		return repository;
	}

	@Bean
	OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
			RegisteredClientRepository registeredClientRepository) {
		return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
	}

	@Bean
	OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
			RegisteredClientRepository registeredClientRepository) {
		return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
	}

	@Bean
	JWKSource<SecurityContext> jwkSource() {
		KeyPair keyPair = generateRsaKey();
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

		RSAKey rsaKey = new RSAKey.Builder(publicKey)
				.privateKey(privateKey)
				.keyID(UUID.randomUUID().toString())
				.build();

		JWKSet jwkSet = new JWKSet(rsaKey);
		return new ImmutableJWKSet<>(jwkSet);
	}

	private static KeyPair generateRsaKey() {
		KeyPair keyPair;
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(2048);
			keyPair = keyPairGenerator.generateKeyPair();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		return keyPair;
	}

	@Bean
	JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
		return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
	}

	@Bean
	OAuth2TokenGenerator<?> tokenGenerator(JWKSource<SecurityContext> jwkSource,
			OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) {
		JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource));
		jwtGenerator.setJwtCustomizer(jwtCustomizer);
		OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
		OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
		return new DelegatingOAuth2TokenGenerator(
				jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
	}

	@Bean
	AuthorizationServerSettings authorizationServerSettings(
			@Value("${spring.security.oauth2.authorizationserver.issuer}") String issuer) {
		return AuthorizationServerSettings.builder()
				.issuer(issuer)
				.build();
	}

}
