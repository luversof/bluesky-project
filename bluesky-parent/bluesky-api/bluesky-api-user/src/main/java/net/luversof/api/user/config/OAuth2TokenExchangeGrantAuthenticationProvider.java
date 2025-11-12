package net.luversof.api.user.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import lombok.Setter;
import net.luversof.api.user.domain.UserInfo;
import net.luversof.api.user.repository.UserInfoRepository;

/**
 * Token Exchange Grant Type을 처리하는 Provider
 * GitHub OAuth token을 받아서 JWT access token으로 교환
 */
public class OAuth2TokenExchangeGrantAuthenticationProvider implements AuthenticationProvider {

    private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc8693#section-2.2.2";
    private static final AuthorizationGrantType TOKEN_EXCHANGE_GRANT_TYPE = new AuthorizationGrantType(
            "urn:ietf:params:oauth:grant-type:token-exchange");

    @Setter(onMethod_ = @Autowired)
    private OAuth2AuthorizationService authorizationService;

    @Setter(onMethod_ = @Autowired)
    private OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;

    @Setter(onMethod_ = @Autowired)
    private UserInfoRepository userInfoRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2TokenExchangeAuthenticationToken tokenExchangeAuthentication = (OAuth2TokenExchangeAuthenticationToken) authentication;

        OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClientElseThrowInvalidClient(
                tokenExchangeAuthentication);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        if (registeredClient == null
                || !registeredClient.getAuthorizationGrantTypes().contains(TOKEN_EXCHANGE_GRANT_TYPE)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }

        // subject_token 추출
        String subjectToken = tokenExchangeAuthentication.getSubjectToken();
        String subjectTokenType = tokenExchangeAuthentication.getSubjectTokenType();

        // token type 검증 (선택적)
        if (!"urn:ietf:params:oauth:token-type:access_token".equals(subjectTokenType)) {
            // 경고: 예상과 다른 토큰 타입
        }

        // GitHub token에서 사용자 정보 조회 (실제로는 GitHub API 호출 필요)
        // 여기서는 간단히 subject_token을 username으로 처리
        UserInfo userInfo = getUserInfoFromGitHubToken(subjectToken);

        // JWT 토큰 생성
        Set<String> authorizedScopes = new HashSet<>(registeredClient.getScopes());

        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(clientPrincipal)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizedScopes(authorizedScopes)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(TOKEN_EXCHANGE_GRANT_TYPE);

        // 추가 claims 설정
        tokenContextBuilder.put("userId", userInfo.getId());
        tokenContextBuilder.put("username", userInfo.getUsername());

        OAuth2TokenContext tokenContext = tokenContextBuilder.build();

        OAuth2Token generatedAccessToken = this.tokenGenerator.generate(tokenContext);
        if (generatedAccessToken == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                            "The token generator failed to generate the access token.", ERROR_URI));
        }

        Jwt jwt = (Jwt) generatedAccessToken;
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), authorizedScopes);

        // Authorization 저장
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(userInfo.getUsername())
                .authorizationGrantType(TOKEN_EXCHANGE_GRANT_TYPE)
                .authorizedScopes(authorizedScopes);

        if (generatedAccessToken instanceof ClaimAccessor) {
            authorizationBuilder.token(accessToken, (metadata) -> {
                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                        ((ClaimAccessor) generatedAccessToken).getClaims());
                metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, false);
            });
        } else {
            authorizationBuilder.accessToken(accessToken);
        }

        OAuth2Authorization authorization = authorizationBuilder.build();
        this.authorizationService.save(authorization);

        return new OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2TokenExchangeAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(
            Authentication authentication) {
        OAuth2ClientAuthenticationToken clientPrincipal = null;
        if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication.getPrincipal().getClass())) {
            clientPrincipal = (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
        }
        if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
            return clientPrincipal;
        }
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }

    /**
     * GitHub token으로 사용자 정보 조회/생성
     * 실제 구현에서는 GitHub API를 호출하여 사용자 정보를 가져와야 함
     */
    private UserInfo getUserInfoFromGitHubToken(String subjectToken) {
        // TODO: GitHub API 호출하여 사용자 정보 가져오기
        // 현재는 임시로 token을 username으로 처리

        // 임시 구현: subject_token에서 username 추출 (실제로는 GitHub API 호출 필요)
        String username = "github_user_" + subjectToken.hashCode();

        return userInfoRepository.findByUsername(username)
                .orElseGet(() -> {
                    UserInfo newUser = new UserInfo();
                    newUser.setUsername(username);
                    newUser.setPassword("{noop}password"); // OAuth 사용자는 비밀번호 불필요
                    return userInfoRepository.save(newUser);
                });
    }
}
