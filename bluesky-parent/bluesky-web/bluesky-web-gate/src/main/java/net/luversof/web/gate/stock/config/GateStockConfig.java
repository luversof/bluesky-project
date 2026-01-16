package net.luversof.web.gate.stock.config;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockAdminClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Configuration
public class GateStockConfig {

	@Bean
	HttpServiceProxyFactory stockHttpServiceProxyFactory(
			Function<String, HttpServiceProxyFactory> httpServiceProxyFactoryBuilder,
			@Value("${spring.http.serviceclient.client-stock.base-url:}") String baseUrl) {
		return httpServiceProxyFactoryBuilder.apply(baseUrl);
	}

	@Bean
	AccountClient accountClient(HttpServiceProxyFactory stockHttpServiceProxyFactory) {
		return stockHttpServiceProxyFactory.createClient(AccountClient.class);
	}

	@Bean
	DividendClient dividendClient(HttpServiceProxyFactory stockHttpServiceProxyFactory) {
		return stockHttpServiceProxyFactory.createClient(DividendClient.class);
	}

	@Bean
	StockAdminClient stockAdminClient(HttpServiceProxyFactory stockHttpServiceProxyFactory) {
		return stockHttpServiceProxyFactory.createClient(StockAdminClient.class);
	}

	@Bean
	StockItemClient stockItemClient(HttpServiceProxyFactory stockHttpServiceProxyFactory) {
		return stockHttpServiceProxyFactory.createClient(StockItemClient.class);
	}

	@Bean
	TradeClient tradeClient(HttpServiceProxyFactory stockHttpServiceProxyFactory) {
		return stockHttpServiceProxyFactory.createClient(TradeClient.class);
	}

	@Bean
	TradeProfitClient tradeProfitClient(HttpServiceProxyFactory stockHttpServiceProxyFactory) {
		return stockHttpServiceProxyFactory.createClient(TradeProfitClient.class);
	}

}
