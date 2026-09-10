package net.luversof.web.gate.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.filter.RequestContextFilter;

/**
 * The request context has to be bound outside Spring Session's filter, otherwise the session lookup
 * that happens as the chain unwinds (see ApiSessionRequestReuseTest) finds no request to reuse from
 * and pays another round trip to the remote session store.
 */
class OuterRequestContextFilterTest {

  @Test
  void bindsTheRequestContextOutsideSpringSession() {
    var registration = new GateRequestContextConfig().outerRequestContextFilter();

    assertThat(registration.getFilter()).isInstanceOf(RequestContextFilter.class);
    assertThat(registration.getOrder())
        .as(
            "must wrap Spring Session's filter, which sits at "
                + SessionRepositoryFilter.DEFAULT_ORDER)
        .isLessThan(SessionRepositoryFilter.DEFAULT_ORDER);
  }
}
