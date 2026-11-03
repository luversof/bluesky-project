package net.luversof.web.gate.bookkeeping.config;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient;
import net.luversof.web.gate.bookkeeping.httpexchange.MemberClient;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient;

/** api-bookkeeping 클라이언트(base-url: spring.http.serviceclient.client-bookkeeping.base-url). */
@Configuration
public class GateBookkeepingConfig {

  @Bean
  HttpServiceProxyFactory bookkeepingHttpServiceProxyFactory(
      Function<String, HttpServiceProxyFactory> httpServiceProxyFactoryBuilder,
      @Value("${spring.http.serviceclient.client-bookkeeping.base-url:}") String baseUrl) {
    return httpServiceProxyFactoryBuilder.apply(baseUrl);
  }

  @Bean
  BookClient bookClient(
      @Qualifier("bookkeepingHttpServiceProxyFactory") HttpServiceProxyFactory factory) {
    return factory.createClient(BookClient.class);
  }

  @Bean
  MemberClient memberClient(
      @Qualifier("bookkeepingHttpServiceProxyFactory") HttpServiceProxyFactory factory) {
    return factory.createClient(MemberClient.class);
  }

  @Bean
  AssetClient assetClient(
      @Qualifier("bookkeepingHttpServiceProxyFactory") HttpServiceProxyFactory factory) {
    return factory.createClient(AssetClient.class);
  }

  @Bean
  CategoryClient categoryClient(
      @Qualifier("bookkeepingHttpServiceProxyFactory") HttpServiceProxyFactory factory) {
    return factory.createClient(CategoryClient.class);
  }

  @Bean
  EntryClient entryClient(
      @Qualifier("bookkeepingHttpServiceProxyFactory") HttpServiceProxyFactory factory) {
    return factory.createClient(EntryClient.class);
  }

  @Bean
  StatsClient statsClient(
      @Qualifier("bookkeepingHttpServiceProxyFactory") HttpServiceProxyFactory factory) {
    return factory.createClient(StatsClient.class);
  }
}
