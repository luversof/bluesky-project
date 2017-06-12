package net.luversof.security.oauth2.config;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import lombok.SneakyThrows;
import net.luversof.security.oauth2.provider.token.BattleNetAccessTokenConverter;

@Configuration
public class OAuth2ClientBattleNetConfig {

	@Autowired
	private OAuth2ClientContext oAuth2ClientContext;

	
	/* (s) battleNet OAuth */
	
	@Value("${oauth2.client.battleNet.clientId}")
	private String battleNetClientId;
	
	@Value("${oauth2.client.battleNet.clientSecret}")
	private String battleNetClientSecret;
	
	@Value("${oauth2.client.battleNet.userAuthorizationUri}")
	private String battleNetUserAuthorizationUri;
	
	@Value("${oauth2.client.battleNet.accessTokenUri}")
	private String battleNetAccessTokenUri;
	
	@Value("${oauth2.client.battleNet.checkTokenEndpointUrl}")
	private String battleNetCheckTokenEndpointUrl;
	
	
	@Bean
	public OAuth2ProtectedResourceDetails battleNetResourceDetails() {
		AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
		details.setId("battleNet");
		details.setClientId(battleNetClientId);
		details.setClientSecret(battleNetClientSecret);
		details.setUserAuthorizationUri(battleNetUserAuthorizationUri);
		details.setAccessTokenUri(battleNetAccessTokenUri);
		details.setScope(Arrays.asList("email"));
		details.setAuthenticationScheme(AuthenticationScheme.form);
		details.setClientAuthenticationScheme(AuthenticationScheme.form);
		return details;
	}

	@Bean
	public OAuth2RestTemplate battleNetRestTemplate() {
		OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(battleNetResourceDetails(), oAuth2ClientContext);
		oAuth2RestTemplate.setAccessTokenProvider(getAccessTokenProvider());
		return oAuth2RestTemplate;
	}
	
	
	@Autowired
	BattleNetAccessTokenConverter battleNetAccessTokenConverter;
	
	@Bean
	public ResourceServerTokenServices battleNetTokenServices(OAuth2RestTemplate battleNetRestTemplate) {
		RemoteTokenServices remoteTokenServices = new RemoteTokenServices() {

			@Override
			public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
				MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
				formData.add("token", accessToken);
				HttpHeaders headers = new HttpHeaders();
				headers.set("Authorization", getAuthorizationHeader(battleNetClientId, battleNetClientSecret));
				if (headers.getContentType() == null) {
					headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> map = battleNetRestTemplate.exchange(battleNetCheckTokenEndpointUrl, HttpMethod.GET,
						new HttpEntity<MultiValueMap<String, String>>(formData, headers), Map.class, accessToken).getBody();

				if (map.containsKey("code")) {
					logger.debug("check_token returned error: " + map.get("detail"));
					throw new InvalidTokenException(accessToken);
				}
				
				@SuppressWarnings("unchecked")
				Map<String, Object> me = battleNetRestTemplate.getForObject("https://kr.api.battle.net/account/user?access_token={accessToken}", Map.class, accessToken);
				map.putAll(me);
				return battleNetAccessTokenConverter.extractAuthentication(map);
			}
			
		};
		remoteTokenServices.setRestTemplate(battleNetRestTemplate());
		remoteTokenServices.setClientId(battleNetClientId);
		remoteTokenServices.setClientSecret(battleNetClientSecret);
		remoteTokenServices.setCheckTokenEndpointUrl(battleNetCheckTokenEndpointUrl);
		return remoteTokenServices;
	}
	
	
	@Bean
	public OAuth2ClientAuthenticationProcessingFilter battleNetAuthenticationProcessingFilter(ResourceServerTokenServices battleNetTokenServices) {
		OAuth2ClientAuthenticationProcessingFilter battleNetAuthenticationProcessingFilter = new OAuth2ClientAuthenticationProcessingFilter("/oauth/battleNetAuthorizeResult");
		battleNetAuthenticationProcessingFilter.setRestTemplate(battleNetRestTemplate());
		battleNetAuthenticationProcessingFilter.setTokenServices(battleNetTokenServices);
		SimpleUrlAuthenticationSuccessHandler simpleUrlAuthenticationSuccessHandler = new SimpleUrlAuthenticationSuccessHandler("/battleNet/d3/index");
		battleNetAuthenticationProcessingFilter.setAuthenticationSuccessHandler(simpleUrlAuthenticationSuccessHandler);
		return battleNetAuthenticationProcessingFilter;
	}
	
	/* (e) battleNet OAuth */
	
	
	@SneakyThrows
	private String getAuthorizationHeader(String clientId, String clientSecret) {
		String creds = String.format("%s:%s", clientId, clientSecret);
		return "Basic " + new String(Base64.getEncoder().encodeToString(creds.getBytes("UTF-8")));
	}
	

	/**
	 * 2.0.8 이후  AuthorizationCodeAccessTokenProvider stateMandatory false 설정이 필요함. 
	 * @return
	 */
	private AccessTokenProvider getAccessTokenProvider() {
		AuthorizationCodeAccessTokenProvider authorizationCodeAccessTokenProvider = new AuthorizationCodeAccessTokenProvider();
		authorizationCodeAccessTokenProvider.setStateMandatory(false);
		return new AccessTokenProviderChain(Arrays.<AccessTokenProvider> asList(
				authorizationCodeAccessTokenProvider, new ImplicitAccessTokenProvider(),
				new ResourceOwnerPasswordAccessTokenProvider(), new ClientCredentialsAccessTokenProvider()));		
	}
}
