package net.luversof.web.gate.poe.config;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import net.luversof.web.gate.poe.httpexchange.PoeBuildClient;
import net.luversof.web.gate.poe.httpexchange.PoeDataClient;
import net.luversof.web.gate.poe.httpexchange.PoeExtractClient;
import net.luversof.web.gate.poe.httpexchange.PoeOptimizeClient;
import net.luversof.web.gate.poe.httpexchange.PoeRegexClient;
import net.luversof.web.gate.poe.httpexchange.PoeSimClient;

@Configuration
public class GatePoeConfig {

  @Bean
  HttpServiceProxyFactory poeHttpServiceProxyFactory(
      Function<String, HttpServiceProxyFactory> httpServiceProxyFactoryBuilder,
      @Value("${spring.http.serviceclient.client-poe.base-url:}") String baseUrl) {
    return httpServiceProxyFactoryBuilder.apply(baseUrl);
  }

  @Bean
  PoeDataClient poeDataClient(
      @Qualifier("poeHttpServiceProxyFactory") HttpServiceProxyFactory poeHttpServiceProxyFactory) {
    return poeHttpServiceProxyFactory.createClient(PoeDataClient.class);
  }

  @Bean
  PoeBuildClient poeBuildClient(
      @Qualifier("poeHttpServiceProxyFactory") HttpServiceProxyFactory poeHttpServiceProxyFactory) {
    return poeHttpServiceProxyFactory.createClient(PoeBuildClient.class);
  }

  @Bean
  PoeOptimizeClient poeOptimizeClient(
      @Qualifier("poeHttpServiceProxyFactory") HttpServiceProxyFactory poeHttpServiceProxyFactory) {
    return poeHttpServiceProxyFactory.createClient(PoeOptimizeClient.class);
  }

  @Bean
  PoeSimClient poeSimClient(
      @Qualifier("poeHttpServiceProxyFactory") HttpServiceProxyFactory poeHttpServiceProxyFactory) {
    return poeHttpServiceProxyFactory.createClient(PoeSimClient.class);
  }

  @Bean
  PoeExtractClient poeExtractClient(
      @Qualifier("poeHttpServiceProxyFactory") HttpServiceProxyFactory poeHttpServiceProxyFactory) {
    return poeHttpServiceProxyFactory.createClient(PoeExtractClient.class);
  }

  @Bean
  PoeRegexClient poeRegexClient(
      @Qualifier("poeHttpServiceProxyFactory") HttpServiceProxyFactory poeHttpServiceProxyFactory) {
    return poeHttpServiceProxyFactory.createClient(PoeRegexClient.class);
  }
}
