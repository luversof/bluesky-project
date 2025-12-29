package net.luversof.web.gate.bookkeeping.config;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetGroupClient;
import net.luversof.web.gate.bookkeeping.httpexchange.BookkeepingClient;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryGroupClient;

@Configuration
public class GateBookkeepingConfig {

	@Bean
	HttpServiceProxyFactory bookkeepingHttpServiceProxyFactory(
			Function<String, HttpServiceProxyFactory> httpServiceProxyFactoryBuilder,
			@Value("${spring.http.serviceclient.client-bookkeeping.base-url:}") String baseUrl) {
		return httpServiceProxyFactoryBuilder.apply(baseUrl);
	}

	@Bean
	AssetClient assetClient(HttpServiceProxyFactory bookkeepingHttpServiceProxyFactory) {
		return bookkeepingHttpServiceProxyFactory.createClient(AssetClient.class);
	}

	@Bean
	AssetGroupClient assetGroupClient(HttpServiceProxyFactory bookkeepingHttpServiceProxyFactory) {
		return bookkeepingHttpServiceProxyFactory.createClient(AssetGroupClient.class);
	}

	@Bean
	BookkeepingClient bookkeepingClient(HttpServiceProxyFactory bookkeepingHttpServiceProxyFactory) {
		return bookkeepingHttpServiceProxyFactory.createClient(BookkeepingClient.class);
	}

	@Bean
	EntryClient entryClient(HttpServiceProxyFactory bookkeepingHttpServiceProxyFactory) {
		return bookkeepingHttpServiceProxyFactory.createClient(EntryClient.class);
	}

	@Bean
	EntryGroupClient entryGroupClient(HttpServiceProxyFactory bookkeepingHttpServiceProxyFactory) {
		return bookkeepingHttpServiceProxyFactory.createClient(EntryGroupClient.class);
	}

}
