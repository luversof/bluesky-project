package net.luversof.web.gate.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.RequestContextFilter;

/**
 * The session store is remote, so every session lookup is a round trip to bluesky-api-user.
 *
 * <p>Spring Session caches the lookup per request, but drops that cache when the response is
 * committed - and a response bigger than the Tomcat output buffer (8KB) commits while the view is
 * still rendering. The session was then looked up a second time as the filter chain unwound, after
 * the servlet had already finished, which is also after Spring Boot's own request-context filter
 * has released the request. ApiSessionRepository reuses a lookup for the rest of the request
 * through the request attributes, so it could not see that second lookup.
 *
 * <p>Binding the request context outside Spring Session's filter keeps the request attributes
 * available for the whole chain, so the second lookup reuses the first instead of paying another
 * round trip. Measured 2026-09-10: fragments of 12,994 / 80,260 / 148,916 bytes each spent two
 * validate-session calls, fragments of 2,843 / 4,295 / 5,586 bytes spent one.
 */
@Configuration
public class GateRequestContextConfig {

  @Bean
  FilterRegistrationBean<RequestContextFilter> outerRequestContextFilter() {
    var registration = new FilterRegistrationBean<>(new RequestContextFilter());
    // Spring Session's filter sits at Integer.MIN_VALUE + 50, so this has to be lower to wrap it.
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
}
