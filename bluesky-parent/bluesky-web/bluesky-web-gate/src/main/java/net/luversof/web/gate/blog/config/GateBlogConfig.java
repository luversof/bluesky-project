package net.luversof.web.gate.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.ImportHttpServices;

import io.github.luversof.boot.web.service.invoker.PageableHttpServiceArgumentResolver;
import net.luversof.web.gate.blog.httpexchange.BlogArticleCategoryClient;
import net.luversof.web.gate.blog.httpexchange.BlogArticleClient;
import net.luversof.web.gate.blog.httpexchange.BlogArticleCommentClient;
import net.luversof.web.gate.blog.httpexchange.BlogClient;

@Configuration
@ImportHttpServices(group = "client-blog", types = {
		BlogArticleCategoryClient.class,
		BlogArticleClient.class,
		BlogArticleCommentClient.class,
		BlogClient.class,
})
public class GateBlogConfig {

	@Bean
	HttpServiceProxyFactory blogHttpServiceProxyFactory(RestClient.Builder builder,
			@Value("${spring.http.serviceclient.client-blog.base-url:}") String baseUrl) {
		RestClient restClient = builder.baseUrl(baseUrl).build();
		return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
				.customArgumentResolver(new PageableHttpServiceArgumentResolver())
				.build();
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