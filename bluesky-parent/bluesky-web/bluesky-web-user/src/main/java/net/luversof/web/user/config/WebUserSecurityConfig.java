package net.luversof.web.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.context.SecurityContextRepository;

/** 앞단 서버 중 로그인을 하는 곳은 여기뿐이다. 결과는 client-user 의 공유 세션(LoginUser)으로 다른 앞단 서버가 읽는다. */
@Configuration
@EnableWebSecurity
public class WebUserSecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      OAuth2LoginSuccessHandler successHandler,
      CustomOAuth2UserService customOAuth2UserService,
      SecurityContextRepository loginUserSecurityContextRepository)
      throws Exception {
    return http.csrf(CsrfConfigurer::disable)
        .securityContext(
            context -> context.securityContextRepository(loginUserSecurityContextRepository))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .oauth2Login(oauth2 ->
            oauth2
                .successHandler(successHandler)
                .failureHandler(
                    new SimpleUrlAuthenticationFailureHandler() {
                      @Override
                      public void onAuthenticationFailure(
                          jakarta.servlet.http.HttpServletRequest request,
                          jakarta.servlet.http.HttpServletResponse response,
                          org.springframework.security.core.AuthenticationException exception)
                          throws java.io.IOException, jakarta.servlet.ServletException {
                        exception.printStackTrace();
                        super.onAuthenticationFailure(request, response, exception);
                      }
                    })
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)))
        .logout(logout ->
            logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true))
        .build();
  }
}
