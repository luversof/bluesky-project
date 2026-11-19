package net.luversof.web.user.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import net.luversof.client.user.config.CustomOAuth2UserService;
import net.luversof.client.user.config.OAuth2LoginSuccessHandler;

@Configuration
@EnableWebSecurity
public class WebUserSecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      ObjectProvider<OAuth2LoginSuccessHandler> successHandlerProvider,
      ObjectProvider<CustomOAuth2UserService> customOAuth2UserServiceProvider)
      throws Exception {
    return http.csrf(CsrfConfigurer::disable)
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .oauth2Login(oauth2 ->
            oauth2
                .successHandler(successHandlerProvider.getObject())
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
                .userInfoEndpoint(
                    userInfo -> userInfo.userService(customOAuth2UserServiceProvider.getObject())))
        .oauth2Client(Customizer.withDefaults())
        .logout(logout ->
            logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true))
        .build();
  }
}
