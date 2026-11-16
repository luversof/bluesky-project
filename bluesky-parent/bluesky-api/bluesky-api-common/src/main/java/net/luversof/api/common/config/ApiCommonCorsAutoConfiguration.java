package net.luversof.api.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * *.bluesky.local 화면(gate · apidocs 등)이 api 를 호출할 수 있게 하는 CORS 설정.
 *
 * <p>각 api 앱은 자기 패키지만 스캔하므로 AutoConfiguration.imports 로 등록한다.
 */
@AutoConfiguration
public class ApiCommonCorsAutoConfiguration implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOriginPatterns("http://*.bluesky.local:[*]", "https://*.bluesky.local:[*]")
        .allowedHeaders("*")
        .allowedMethods("*")
        .allowCredentials(true);
  }
}
