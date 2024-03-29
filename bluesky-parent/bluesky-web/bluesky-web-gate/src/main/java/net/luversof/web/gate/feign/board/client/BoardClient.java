package net.luversof.web.gate.feign.board.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.feign.board.domain.Board;


@FeignClient(value = "bluesky-api-board", contextId = "api-board", path = "/api/board", url = "${gate.feign-client.url.board:}")
public interface BoardClient {
	
	@PostMapping
	Board create(@RequestBody Board board);
	
	@GetMapping("/findByAlias")
	Board findByAlias(@RequestParam String alias);
	
	@GetMapping("/findAll")
	Page<Board> findAll(Pageable pageable);
	
	@PutMapping
	Board update(@RequestBody Board board);

}
