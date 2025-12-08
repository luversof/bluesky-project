package net.luversof.web.gate.board.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

import net.luversof.web.gate.board.httpexchange.BoardArticleClient;
import net.luversof.web.gate.board.httpexchange.BoardArticleCommentClient;
import net.luversof.web.gate.board.httpexchange.BoardClient;

@Configuration
@ImportHttpServices(group = "client-board", types = { 
		BoardArticleClient.class,
		BoardArticleCommentClient.class,
		BoardClient.class,
})
public class GateBoardConfig {

}
