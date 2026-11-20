package net.luversof.client.user.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;

/** 앞단 서버 공통: 공유 세션에서 로그인 여부를 읽는다. 로그인 자체는 web-user 만 한다. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
  ClientUserHttpExchangeConfig.class,
  ClientUserSecurityConfig.class,
  ApiSessionConfig.class
})
@PropertySource("classpath:clientUser.properties")
public class ClientUserAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
    FilterRegistrationBean<ForwardedHeaderFilter> bean =
        new FilterRegistrationBean<>(new ForwardedHeaderFilter());
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
  }
}
