package net.luversof.client.common.config;

import java.util.function.Function;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import io.github.luversof.boot.web.client.BlueskyClientResponseErrorHandler;
import io.github.luversof.boot.web.service.invoker.PageableHttpServiceArgumentResolver;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration
public class ClientCommonAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	RestClient restClient(RestClient.Builder builder) {
		return builder.build();
	}

	@Bean
	RestClientCustomizer clientCommonRestClientCustomizer(JsonMapper jsonMapper) {
		return (builder) -> {
			builder.defaultStatusHandler(new BlueskyClientResponseErrorHandler(jsonMapper));
		};
	}

	@Bean
	@ConditionalOnMissingBean
	PageableHttpServiceArgumentResolver pageableHttpServiceArgumentResolver() {
		return new PageableHttpServiceArgumentResolver();
	}

	@Bean
	@ConditionalOnMissingBean
	Function<String, HttpServiceProxyFactory> httpServiceProxyFactoryBuilder(ObjectProvider<RestClient.Builder> builderProvider,
			PageableHttpServiceArgumentResolver pageableHttpServiceArgumentResolver) {
		return baseUrl -> {
			RestClient restClient = builderProvider.getObject().baseUrl(baseUrl).build();
			return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
					.customArgumentResolver(pageableHttpServiceArgumentResolver)
					.build();
		};
	}

	@Bean
	HttpServiceProxyFactory clientCommonHttpServiceProxyFactory(RestClient restClient,
			PageableHttpServiceArgumentResolver pageableHttpServiceArgumentResolver) {
		return HttpServiceProxyFactory
				.builderFor(RestClientAdapter.create(restClient))
				.customArgumentResolver(pageableHttpServiceArgumentResolver)
				.build();
	}

}
