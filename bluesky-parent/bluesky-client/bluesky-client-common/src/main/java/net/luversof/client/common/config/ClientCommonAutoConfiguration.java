package net.luversof.client.common.config;

import java.util.function.Function;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
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
    @Scope("prototype")
    @ConditionalOnMissingBean
    @Primary
    RestClient.Builder restClientBuilder(RestClientBuilderConfigurer restClientBuilderConfigurer) {
        return restClientBuilderConfigurer.configure(RestClient.builder());
    }

    @Bean
    @Scope("prototype")
    @LoadBalanced
    RestClient.Builder blueskyRestClientBuilder(
            RestClientBuilderConfigurer restClientBuilderConfigurer) {
        return restClientBuilderConfigurer.configure(RestClient.builder());
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
    Function<String, HttpServiceProxyFactory> httpServiceProxyFactoryBuilder(
            @LoadBalanced ObjectProvider<RestClient.Builder> loadBalancedRestClientBuilderProvider,
            ObjectProvider<RestClient.Builder> restClientBuilderProvider,
            PageableHttpServiceArgumentResolver pageableHttpServiceArgumentResolver) {
        return baseUrl -> {
            RestClient.Builder builder;
            if (baseUrl.startsWith("lb://")) {
                builder = loadBalancedRestClientBuilderProvider.getObject();
            } else {
                builder = restClientBuilderProvider.getObject();
            }

            RestClient restClient = builder.baseUrl(baseUrl).build();
            return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                    .customArgumentResolver(pageableHttpServiceArgumentResolver)
                    .build();
        };
    }

    @Bean
    HttpServiceProxyFactory clientCommonHttpServiceProxyFactory(
            RestClient restClient,
            PageableHttpServiceArgumentResolver pageableHttpServiceArgumentResolver) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .customArgumentResolver(pageableHttpServiceArgumentResolver)
                .build();
    }
}
