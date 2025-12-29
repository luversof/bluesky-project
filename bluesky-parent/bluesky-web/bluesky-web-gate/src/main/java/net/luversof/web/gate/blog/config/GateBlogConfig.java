package net.luversof.web.gate.blog.config;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import net.luversof.web.gate.blog.httpexchange.BlogArticleCategoryClient;
import net.luversof.web.gate.blog.httpexchange.BlogArticleClient;
import net.luversof.web.gate.blog.httpexchange.BlogArticleCommentClient;
import net.luversof.web.gate.blog.httpexchange.BlogClient;

@Configuration
public class GateBlogConfig {

	@Bean
	HttpServiceProxyFactory blogHttpServiceProxyFactory(
			Function<String, HttpServiceProxyFactory> httpServiceProxyFactoryBuilder,
			@Value("${spring.http.serviceclient.client-blog.base-url:}") String baseUrl) {
		return httpServiceProxyFactoryBuilder.apply(baseUrl);
	}

	@Bean
	BlogArticleCategoryClient blogArticleCategoryClient(HttpServiceProxyFactory blogHttpServiceProxyFactory) {
		return blogHttpServiceProxyFactory.createClient(BlogArticleCategoryClient.class);
	}

	@Bean
	BlogArticleClient blogArticleClient(HttpServiceProxyFactory blogHttpServiceProxyFactory) {
		return blogHttpServiceProxyFactory.createClient(BlogArticleClient.class);
	}

	@Bean
	BlogArticleCommentClient blogArticleCommentClient(HttpServiceProxyFactory blogHttpServiceProxyFactory) {
		return blogHttpServiceProxyFactory.createClient(BlogArticleCommentClient.class);
	}

	@Bean
	BlogClient blogClient(HttpServiceProxyFactory blogHttpServiceProxyFactory) {
		return blogHttpServiceProxyFactory.createClient(BlogClient.class);
	}

}