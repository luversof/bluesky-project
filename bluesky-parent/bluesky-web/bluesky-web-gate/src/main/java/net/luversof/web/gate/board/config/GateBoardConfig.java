package net.luversof.web.gate.board.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.ImportHttpServices;

import io.github.luversof.boot.web.service.invoker.PageableHttpServiceArgumentResolver;
import net.luversof.web.gate.board.httpexchange.BoardArticleClient;
import net.luversof.web.gate.board.httpexchange.BoardArticleCommentClient;
import net.luversof.web.gate.board.httpexchange.BoardClient;

@Configuration
@ImportHttpServices(group = "client-board", types = {
		BoardArticleClient.class,
		BoardArticleCommentClient.class,
		BoardClient.class
})
public class GateBoardConfig {

	@Bean
	HttpServiceProxyFactory boardHttpServiceProxyFactory(RestClient.Builder builder,
			@Value("${spring.http.serviceclient.client-board.base-url:}") String baseUrl) {
		RestClient restClient = builder.baseUrl(baseUrl).build();
		return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
				.customArgumentResolver(new PageableHttpServiceArgumentResolver())
				.build();
	}

	@Bean
	BoardArticleClient boardArticleClient(HttpServiceProxyFactory boardHttpServiceProxyFactory) {
		return boardHttpServiceProxyFactory.createClient(BoardArticleClient.class);
	}

	@Bean
	BoardArticleCommentClient boardArticleCommentClient(HttpServiceProxyFactory boardHttpServiceProxyFactory) {
		return boardHttpServiceProxyFactory.createClient(BoardArticleCommentClient.class);
	}

	@Bean
	BoardClient boardClient(HttpServiceProxyFactory boardHttpServiceProxyFactory) {
		return boardHttpServiceProxyFactory.createClient(BoardClient.class);
	}

}
