package net.luversof.web.gate.board.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "bluesky-api-board", contextId = "api-board", path = "/api/board", url = "${gate.feign-client.url.board:}")
public interface BoardArticleClient {

	
}
