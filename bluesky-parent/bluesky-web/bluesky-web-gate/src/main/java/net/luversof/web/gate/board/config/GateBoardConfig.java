package net.luversof.web.gate.board.config;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import net.luversof.web.gate.board.httpexchange.BoardArticleClient;
import net.luversof.web.gate.board.httpexchange.BoardArticleCommentClient;
import net.luversof.web.gate.board.httpexchange.BoardClient;

@Configuration
public class GateBoardConfig {

	@Bean
	HttpServiceProxyFactory boardHttpServiceProxyFactory(
			Function<String, HttpServiceProxyFactory> httpServiceProxyFactoryBuilder,
			@Value("${spring.http.serviceclient.client-board.base-url:}") String baseUrl) {
		return httpServiceProxyFactoryBuilder.apply(baseUrl);
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
