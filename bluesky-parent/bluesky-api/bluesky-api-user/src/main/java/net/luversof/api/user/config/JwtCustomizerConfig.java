package net.luversof.api.user.config;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import net.luversof.api.user.repository.UserInfoRepository;

@Configuration
public class JwtCustomizerConfig {

  private UserInfoRepository userInfoRepository;

  @Autowired
  public void setUserInfoRepository(UserInfoRepository userInfoRepository) {
    this.userInfoRepository = userInfoRepository;
  }

  @Bean
  OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
    return context -> {
      if (context.getTokenType().getValue().equals("access_token")) {
        var principal = context.getPrincipal();
        String username = principal.getName();

        // UserInfo 조회하여 userId 추가
        try {
          var userInfoOptional = userInfoRepository.findByUsername(username);
          if (userInfoOptional.isPresent()) {
            var userInfo = userInfoOptional.get();

            // userId를 sub claim에 추가 (표준)
            context.getClaims().claim("sub", userInfo.getId().toString());

            // username도 추가
            context.getClaims().claim("username", userInfo.getUsername());
            context.getClaims().claim("preferred_username", userInfo.getUsername());
          }
        } catch (Exception e) {
          // UserInfo 조회 실패 시 기본값 사용
          context.getClaims().claim("username", username);
          context.getClaims().claim("preferred_username", username);
        }

        // authorities를 scope로 변환
        Set<String> authorities =
            principal.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        context.getClaims().claim("authorities", authorities);
      }
    };
  }
}
