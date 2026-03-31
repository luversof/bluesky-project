package net.luversof.client.user.config;

import java.util.function.Function;
import net.luversof.client.user.httpexchange.UserInfoApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientUserHttpExchangeConfig {

    @Bean
    @ConditionalOnMissingBean(name = "clientUserHttpServiceProxyFactory")
    HttpServiceProxyFactory clientUserHttpServiceProxyFactory(
            Function<String, HttpServiceProxyFactory> httpServiceProxyFactoryBuilder,
            @Value("${spring.http.serviceclient.client-user.base-url}") String baseUrl) {
        return httpServiceProxyFactoryBuilder.apply(baseUrl);
    }

    @Bean
    UserInfoApiClient userInfoApiClient(HttpServiceProxyFactory clientUserHttpServiceProxyFactory) {
        return clientUserHttpServiceProxyFactory.createClient(UserInfoApiClient.class);
    }
}
