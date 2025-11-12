# Bluesky 프로젝트 인증/인가 아키텍처 개선 계획

## 현재 아키텍처 문제점

### 1. 분산된 인증 로직
- OAuth2 인증이 `bluesky-api-user`에서 처리됨
- 각 서비스가 FeignClient로 사용자 정보 조회
- UserUtil에서 SecurityContext 직접 접근하여 UserInfo 조회

### 2. 세션 기반 인증의 한계
- Spring Session + Redis 사용
- 마이크로서비스 환경에서 확장성 제한
- 서비스 간 인증 상태 공유 복잡

### 3. 토큰 관리 부재
- OAuth2 Access Token을 중앙에서 관리하지 않음
- 토큰 갱신 로직 분산
- API 간 호출 시 인증 정보 전파 어려움

## 개선 목표

### 표준 OAuth 2.0 / OpenID Connect 구조
```
┌─────────────────────────────────────────────────────────────┐
│                    사용자 (User)                               │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────┐
│           bluesky-web-gate (Client Application)              │
│  - OAuth2 Client 역할                                         │
│  - Authorization Code Flow                                   │
│  - JWT Token 기반 인증                                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ↓ (Authorization Request)
┌─────────────────────────────────────────────────────────────┐
│      bluesky-authorization-server (Authorization Server)     │
│  - Spring Authorization Server 사용                           │
│  - 인증 처리 (OAuth2, Form Login 등)                         │
│  - JWT Access Token 발급                                     │
│  - Refresh Token 관리                                        │
│  - UserInfo Endpoint 제공                                    │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ↓ (Token with JWT)
┌─────────────────────────────────────────────────────────────┐
│              Resource Servers (API 서비스들)                  │
│  - bluesky-api-board                                         │
│  - bluesky-api-stock                                         │
│  - bluesky-api-user (UserInfo 관리만)                        │
│  - JWT Token 검증 (Resource Server)                          │
└─────────────────────────────────────────────────────────────┘
```

## 상세 구현 계획

### Phase 1: Authorization Server 구축

#### 1.1 새 모듈 생성
```
bluesky-parent/
  └── bluesky-authorization-server/
      ├── pom.xml
      └── src/main/java/net/luversof/authorization/
          ├── config/
          │   ├── AuthorizationServerConfig.java
          │   ├── SecurityConfig.java
          │   └── JwtConfig.java
          ├── domain/
          │   └── User.java (기존 UserInfo 재사용)
          ├── service/
          │   ├── CustomUserDetailsService.java
          │   └── OAuth2ClientService.java
          └── Application.java
```

#### 1.2 핵심 의존성 (pom.xml)
```xml
<dependencies>
    <!-- Spring Authorization Server -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-oauth2-authorization-server</artifactId>
    </dependency>
    
    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- OAuth2 Client (소셜 로그인용) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>
    
    <!-- JDBC -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jdbc</artifactId>
    </dependency>
    
    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
</dependencies>
```

#### 1.3 Authorization Server 설정
```java
@Configuration
public class AuthorizationServerConfig {
    
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) 
            throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults()); // OpenID Connect 1.0 지원
        
        http
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
            )
            .oauth2ResourceServer(resourceServer -> 
                resourceServer.jwt(Customizer.withDefaults()));
        
        return http.build();
    }
    
    @Bean
    public RegisteredClientRepository registeredClientRepository(
            JdbcTemplate jdbcTemplate) {
        // DB 기반 Client 등록 정보 관리
        JdbcRegisteredClientRepository repository = 
            new JdbcRegisteredClientRepository(jdbcTemplate);
        
        // bluesky-web-gate 클라이언트 등록
        RegisteredClient webGateClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("bluesky-web-gate")
            .clientSecret("{noop}secret") // 실제로는 암호화 필요
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("https://dev.bluesky.local:30122/login/oauth2/code/bluesky")
            .postLogoutRedirectUri("https://dev.bluesky.local:30122/")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope("board.read")
            .scope("board.write")
            .scope("stock.read")
            .scope("stock.write")
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(1))
                .refreshTokenTimeToLive(Duration.ofDays(30))
                .build())
            .build();
        
        repository.save(webGateClient);
        
        return repository;
    }
    
    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate, 
            RegisteredClientRepository registeredClientRepository) {
        // DB 기반 Authorization 정보 관리
        return new JdbcOAuth2AuthorizationService(
            jdbcTemplate, 
            registeredClientRepository
        );
    }
    
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate, 
            RegisteredClientRepository registeredClientRepository) {
        // DB 기반 동의 정보 관리
        return new JdbcOAuth2AuthorizationConsentService(
            jdbcTemplate, 
            registeredClientRepository
        );
    }
    
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // JWT 서명용 키 생성
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
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }
    
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer("https://auth.bluesky.local:30140")
            .build();
    }
}
```

#### 1.4 일반 Security 설정 (폼 로그인 + 소셜 로그인)
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) 
            throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/assets/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(formLogin -> formLogin
                .loginPage("/login")
                .permitAll()
            )
            .oauth2Login(oauth2Login -> oauth2Login
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .userAuthoritiesMapper(grantedAuthoritiesMapper())
                )
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            );
        
        return http.build();
    }
    
    @Bean
    public UserDetailsService userDetailsService(UserInfoService userInfoService) {
        return username -> {
            UserInfo userInfo = userInfoService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            return User.builder()
                .username(userInfo.username())
                .password(userInfo.password())
                .authorities("ROLE_USER")
                .build();
        };
    }
    
    @Bean
    public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            
            // OAuth2 provider별 추가 권한 매핑
            authorities.forEach(authority -> {
                if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
                    // 필요시 추가 권한 매핑
                }
            });
            
            return mappedAuthorities;
        };
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
```

### Phase 2: Web Gateway 변경 (OAuth2 Client)

#### 2.1 bluesky-web-gate Security 설정
```java
@Configuration
@EnableWebSecurity
public class GateSecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/assets/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
            )
            .oauth2Client(Customizer.withDefaults())
            .logout(logout -> logout
                .logoutSuccessUrl("/")
            );
        
        return http.build();
    }
    
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {
        
        OAuth2AuthorizedClientProvider authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build();
        
        DefaultOAuth2AuthorizedClientManager authorizedClientManager =
            new DefaultOAuth2AuthorizedClientManager(
                clientRegistrationRepository, 
                authorizedClientRepository
            );
        
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        
        return authorizedClientManager;
    }
}
```

#### 2.2 application.properties 설정
```properties
# OAuth2 Client 설정
spring.security.oauth2.client.registration.bluesky.client-id=bluesky-web-gate
spring.security.oauth2.client.registration.bluesky.client-secret=secret
spring.security.oauth2.client.registration.bluesky.scope=openid,profile,board.read,board.write,stock.read,stock.write
spring.security.oauth2.client.registration.bluesky.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.bluesky.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}

spring.security.oauth2.client.provider.bluesky.issuer-uri=https://auth.bluesky.local:30140
spring.security.oauth2.client.provider.bluesky.authorization-uri=https://auth.bluesky.local:30140/oauth2/authorize
spring.security.oauth2.client.provider.bluesky.token-uri=https://auth.bluesky.local:30140/oauth2/token
spring.security.oauth2.client.provider.bluesky.user-info-uri=https://auth.bluesky.local:30140/userinfo
spring.security.oauth2.client.provider.bluesky.jwk-set-uri=https://auth.bluesky.local:30140/oauth2/jwks
spring.security.oauth2.client.provider.bluesky.user-name-attribute=sub
```

#### 2.3 FeignClient에 OAuth2 Token 전파
```java
@Configuration
public class GateFeignConfig {
    
    @Bean
    public RequestInterceptor oauth2FeignRequestInterceptor(
            OAuth2AuthorizedClientManager authorizedClientManager) {
        
        return requestTemplate -> {
            Authentication authentication = 
                SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                OAuth2AuthorizeRequest authorizeRequest = 
                    OAuth2AuthorizeRequest.withClientRegistrationId("bluesky")
                        .principal(authentication)
                        .build();
                
                OAuth2AuthorizedClient authorizedClient = 
                    authorizedClientManager.authorize(authorizeRequest);
                
                if (authorizedClient != null) {
                    String accessToken = authorizedClient.getAccessToken().getTokenValue();
                    requestTemplate.header("Authorization", "Bearer " + accessToken);
                }
            }
        };
    }
}
```

### Phase 3: Resource Server 설정 (API 서비스들)

#### 3.1 각 API 서비스 Security 설정
```java
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {
    
    @Bean
    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) 
            throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .csrf(csrf -> csrf.disable());
        
        return http.build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        // Authorization Server의 JWK Set URI 사용
        return JwtDecoders.fromIssuerLocation("https://auth.bluesky.local:30140");
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = 
            new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("scope");
        grantedAuthoritiesConverter.setAuthorityPrefix("SCOPE_");
        
        JwtAuthenticationConverter jwtAuthenticationConverter = 
            new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            grantedAuthoritiesConverter
        );
        
        return jwtAuthenticationConverter;
    }
}
```

#### 3.2 application.properties 설정
```properties
# Resource Server 설정
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.bluesky.local:30140
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth.bluesky.local:30140/oauth2/jwks
```

### Phase 4: UserUtil 개선

#### 4.1 JWT 기반 UserUtil
```java
@UtilityClass
public class UserUtil {
    
    public static UUID getUserId() {
        Authentication authentication = 
            SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            // JWT의 sub claim에서 userId 추출
            Jwt jwt = jwtAuth.getToken();
            String userId = jwt.getSubject();
            return UUID.fromString(userId);
        }
        
        if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
            // OAuth2 로그인인 경우 (web-gate)
            OAuth2User principal = oauth2Auth.getPrincipal();
            String userId = principal.getAttribute("sub");
            return UUID.fromString(userId);
        }
        
        return null;
    }
    
    public static String getUsername() {
        Authentication authentication = 
            SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaim("username");
        }
        
        if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
            OAuth2User principal = oauth2Auth.getPrincipal();
            return principal.getAttribute("preferred_username");
        }
        
        return null;
    }
    
    // FeignClient 호출 불필요! JWT에서 직접 추출
    public static Map<UUID, String> getUsernames(List<UUID> userIds) {
        // 이 메서드는 여전히 필요하지만, 캐싱 전략으로 개선 가능
        var userInfoClient = ApplicationContextUtil.getApplicationContext()
            .getBean(UserInfoClient.class);
        
        Map<UUID, String> usernames = new HashMap<>();
        
        for (UUID userId : userIds) {
            try {
                var userInfoOptional = userInfoClient.findById(userId);
                if (userInfoOptional.isPresent()) {
                    var userInfo = userInfoOptional.get();
                    usernames.put(userId, 
                        userInfo.username() != null ? userInfo.username() : "알 수 없음");
                }
            } catch (Exception e) {
                // 개별 조회 실패 시 건너뜀
            }
        }
        
        return usernames;
    }
}
```

## 데이터베이스 스키마

### Authorization Server용 테이블
```sql
-- Spring Authorization Server 기본 스키마
-- https://github.com/spring-projects/spring-authorization-server/blob/main/oauth2-authorization-server/src/main/resources/org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql

CREATE TABLE oauth2_registered_client (
    id varchar(100) NOT NULL,
    client_id varchar(100) NOT NULL,
    client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret varchar(200) DEFAULT NULL,
    client_secret_expires_at timestamp DEFAULT NULL,
    client_name varchar(200) NOT NULL,
    client_authentication_methods varchar(1000) NOT NULL,
    authorization_grant_types varchar(1000) NOT NULL,
    redirect_uris varchar(1000) DEFAULT NULL,
    post_logout_redirect_uris varchar(1000) DEFAULT NULL,
    scopes varchar(1000) NOT NULL,
    client_settings varchar(2000) NOT NULL,
    token_settings varchar(2000) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE oauth2_authorization (
    id varchar(100) NOT NULL,
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorization_grant_type varchar(100) NOT NULL,
    authorized_scopes varchar(1000) DEFAULT NULL,
    attributes text DEFAULT NULL,
    state varchar(500) DEFAULT NULL,
    authorization_code_value text DEFAULT NULL,
    authorization_code_issued_at timestamp DEFAULT NULL,
    authorization_code_expires_at timestamp DEFAULT NULL,
    authorization_code_metadata text DEFAULT NULL,
    access_token_value text DEFAULT NULL,
    access_token_issued_at timestamp DEFAULT NULL,
    access_token_expires_at timestamp DEFAULT NULL,
    access_token_metadata text DEFAULT NULL,
    access_token_type varchar(100) DEFAULT NULL,
    access_token_scopes varchar(1000) DEFAULT NULL,
    oidc_id_token_value text DEFAULT NULL,
    oidc_id_token_issued_at timestamp DEFAULT NULL,
    oidc_id_token_expires_at timestamp DEFAULT NULL,
    oidc_id_token_metadata text DEFAULT NULL,
    refresh_token_value text DEFAULT NULL,
    refresh_token_issued_at timestamp DEFAULT NULL,
    refresh_token_expires_at timestamp DEFAULT NULL,
    refresh_token_metadata text DEFAULT NULL,
    user_code_value text DEFAULT NULL,
    user_code_issued_at timestamp DEFAULT NULL,
    user_code_expires_at timestamp DEFAULT NULL,
    user_code_metadata text DEFAULT NULL,
    device_code_value text DEFAULT NULL,
    device_code_issued_at timestamp DEFAULT NULL,
    device_code_expires_at timestamp DEFAULT NULL,
    device_code_metadata text DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorities varchar(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);
```

## 마이그레이션 단계

### Step 1: Authorization Server 구축 및 테스트
1. bluesky-authorization-server 모듈 생성
2. 기본 폼 로그인 구현
3. OAuth2 클라이언트 등록
4. 토큰 발급 테스트

### Step 2: Web Gateway OAuth2 Client 전환
1. bluesky-web-gate에 OAuth2 Client 설정 추가
2. 기존 세션 로그인과 병행 운영
3. 점진적 마이그레이션

### Step 3: Resource Server 전환
1. bluesky-api-board부터 시작
2. JWT 검증 로직 추가
3. 다른 API 서비스 순차적 전환

### Step 4: 기존 인증 로직 제거
1. bluesky-api-user의 OAuth2 로직 제거
2. UserUtil 간소화
3. 불필요한 FeignClient 호출 제거

## 기대 효과

1. **표준 준수**: OAuth 2.0 / OpenID Connect 표준 완전 준수
2. **확장성**: JWT 기반으로 stateless 인증, 수평 확장 용이
3. **보안 강화**: 중앙 집중식 토큰 관리, 토큰 갱신 자동화
4. **성능 개선**: UserInfo 조회를 위한 불필요한 FeignClient 호출 감소
5. **유지보수성**: 인증 로직 중앙화, 각 서비스는 토큰 검증만 수행

## 참고 자료

- [Spring Authorization Server 공식 문서](https://docs.spring.io/spring-authorization-server/docs/current/reference/html/)
- [OAuth 2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
