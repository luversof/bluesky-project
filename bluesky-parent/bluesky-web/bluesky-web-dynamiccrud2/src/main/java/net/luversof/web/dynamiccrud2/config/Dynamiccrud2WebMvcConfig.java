package net.luversof.web.dynamiccrud2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import net.luversof.web.dynamiccrud2.security.SettingAccessInterceptor;

@Configuration
public class Dynamiccrud2WebMvcConfig implements WebMvcConfigurer {

  private final SettingAccessInterceptor settingAccessInterceptor;

  public Dynamiccrud2WebMvcConfig(SettingAccessInterceptor settingAccessInterceptor) {
    this.settingAccessInterceptor = settingAccessInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(settingAccessInterceptor)
        .addPathPatterns(SettingAccessInterceptor.PATH_PATTERN);
  }
}
