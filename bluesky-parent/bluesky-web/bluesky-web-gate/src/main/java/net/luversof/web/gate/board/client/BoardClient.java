package net.luversof.web.gate.board.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.board.domain.Board;


@FeignClient(value = "bluesky-api-board", contextId = "api-board", path = "/api/board", url = "${gate.feign-client.url.board:}")
public interface BoardClient {
	
	@PostMapping
	Board create(@RequestBody Board board);
	
	@GetMapping("/findByAlias")
	Board findByAlias(@RequestParam String alias);
	
	@PutMapping
	Board update(@RequestBody Board board);

}
