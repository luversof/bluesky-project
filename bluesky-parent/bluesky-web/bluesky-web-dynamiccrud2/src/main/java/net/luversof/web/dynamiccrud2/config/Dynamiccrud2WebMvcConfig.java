package net.luversof.web.dynamiccrud2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import net.luversof.web.dynamiccrud2.security.AdminAccessInterceptor;

@Configuration
public class Dynamiccrud2WebMvcConfig implements WebMvcConfigurer {

  private final AdminAccessInterceptor adminAccessInterceptor;

  public Dynamiccrud2WebMvcConfig(AdminAccessInterceptor adminAccessInterceptor) {
    this.adminAccessInterceptor = adminAccessInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(adminAccessInterceptor)
        .addPathPatterns(AdminAccessInterceptor.PATH_PATTERN);
  }
}
