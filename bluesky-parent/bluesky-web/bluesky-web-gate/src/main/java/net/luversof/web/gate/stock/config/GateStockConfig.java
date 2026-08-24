package net.luversof.web.gate.stock.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.ActivityFilterIdsClient;
import net.luversof.web.gate.stock.httpexchange.DataFirstDateClient;
import net.luversof.web.gate.stock.httpexchange.DataStatusClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendPayoutClient;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendProfileClient;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendSnapshotClient;
import net.luversof.web.gate.stock.httpexchange.StockAdminClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Configuration
public class GateStockConfig {

  /**
   * 한 화면을 그리는 데 필요한 서로 독립적인 api-stock 호출을 동시에 던지기 위한 실행기.
   *
   * <p>RestClient 인터셉터가 SecurityContextHolder(ThreadLocal) 에서 토큰을 꺼내므로 반드시
   * DelegatingSecurityContextExecutorService 로 감싸야 한다. 감싸지 않으면 호출은 성공하지만 Authorization 헤더가 조용히 빠진다.
   *
   * <p>작업이 전부 I/O 대기라 가상 스레드를 쓴다(요청당 몇 개, 풀 크기 제한 불필요).
   */
  @Bean(destroyMethod = "shutdown")
  ExecutorService stockRemoteCallExecutor() {
    return new DelegatingSecurityContextExecutorService(
        Executors.newVirtualThreadPerTaskExecutor());
  }

  @Bean
  HttpServiceProxyFactory stockHttpServiceProxyFactory(
      Function<String, HttpServiceProxyFactory> httpServiceProxyFactoryBuilder,
      @Value("${spring.http.serviceclient.client-stock.base-url:}") String baseUrl) {
    return httpServiceProxyFactoryBuilder.apply(baseUrl);
  }

  @Bean
  AccountClient accountClient(
      @Qualifier("stockHttpServiceProxyFactory")
          HttpServiceProxyFactory stockHttpServiceProxyFactory) {
    return stockHttpServiceProxyFactory.createClient(AccountClient.class);
  }

  @Bean
  DataStatusClient dataStatusClient(
      @Qualifier("stockHttpServiceProxyFactory")
          HttpServiceProxyFactory stockHttpServiceProxyFactory) {
    return stockHttpServiceProxyFactory.createClient(DataStatusClient.class);
  }

  @Bean
  net.luversof.web.gate.stock.httpexchange.LedgerIntegrityClient ledgerIntegrityClient(
      @Qualifier("stockHttpServiceProxyFactory")
          HttpServiceProxyFactory stockHttpServiceProxyFactory) {
    return stockHttpServiceProxyFactory.createClient(
        net.luversof.web.gate.stock.httpexchange.LedgerIntegrityClient.class);
  }

  @Bean
  ActivityFilterIdsClient activityFilterIdsClient(
      @Qualifier("stockHttpServiceProxyFactory")
          HttpServiceProxyFactory stockHttpServiceProxyFactory) {
    return stockHttpServiceProxyFactory.createClient(ActivityFilterIdsClient.class);
  }

  @Bean
  DataFirstDateClient dataFirstDateClient(
      @Qualifier("stockHttpServiceProxyFactory")
          HttpServiceProxyFactory stockHttpServiceProxyFactory) {
    return stockHttpServiceProxyFactory.createClient(DataFirstDateClient.class);
  }

  @Bean
  DividendClient dividendClient(
      @Qualifier("stockHttpServiceProxyFactory")
          HttpServiceProxyFactory stockHttpServiceProxyFactory) {
    return stockHttpServiceProxyFactory.createClient(DividendClient.class);
  }

  @Bean
  StockAdminClient stockAdminClient(
      @Qualifier("stockHttpServiceProxyFactory")
          HttpServiceProxyFactory stockHttpServiceProxyFactory) {
    return stockHttpServiceProxyFactory.createClient(StockAdminClient.class);
  }

  @Bean
  StockItemClient stockItemClient(
      @Qualifier("stockHttpServiceProxyFactory")
          HttpServiceProxyFactory stockHttpServiceProxyFactory) {
    return stockHttpServiceProxyFactory.createClient(StockItemClient.class);
  }

  @Bean
  MonthlyDividendSnapshotClient monthlyDividendSnapshotClient(
      @Qualifier("stockHttpServiceProxyFactory")
          HttpServiceProxyFactory stockHttpServiceProxyFactory) {
    return stockHttpServiceProxyFactory.createClient(MonthlyDividendSnapshotClient.class);
  }

  @Bean
  MonthlyDividendProfileClient monthlyDividendProfileClient(
      @Qualifier("stockHttpServiceProxyFactory")
          HttpServiceProxyFactory stockHttpServiceProxyFactory) {
    return stockHttpServiceProxyFactory.createClient(MonthlyDividendProfileClient.class);
  }

  @Bean
  MonthlyDividendPayoutClient monthlyDividendPayoutClient(
      @Qualifier("stockHttpServiceProxyFactory")
          HttpServiceProxyFactory stockHttpServiceProxyFactory) {
    return stockHttpServiceProxyFactory.createClient(MonthlyDividendPayoutClient.class);
  }

  @Bean
  TradeClient tradeClient(
      @Qualifier("stockHttpServiceProxyFactory")
          HttpServiceProxyFactory stockHttpServiceProxyFactory) {
    return stockHttpServiceProxyFactory.createClient(TradeClient.class);
  }

  @Bean
  TradeProfitClient tradeProfitClient(
      @Qualifier("stockHttpServiceProxyFactory")
          HttpServiceProxyFactory stockHttpServiceProxyFactory) {
    return stockHttpServiceProxyFactory.createClient(TradeProfitClient.class);
  }
}
