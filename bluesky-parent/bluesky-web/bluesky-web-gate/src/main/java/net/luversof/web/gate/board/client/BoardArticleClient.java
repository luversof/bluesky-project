package net.luversof.web.gate.board.client;

import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.board.domain.BoardArticle;


@FeignClient(value = "bluesky-api-board", contextId = "api-board-article", path = "/api/board/article", url = "${gate.feign-client.url.board:}")
public interface BoardArticleClient {

	@PostMapping
	BoardArticle create(@RequestBody BoardArticle boardArticle);
	
	@GetMapping("/findByBoardAlias")
	Page<BoardArticle> findByBoardAlias(@RequestParam String boardAlias, @RequestParam int page);
	
	@GetMapping("/findByBoardArticleId")
	Optional<BoardArticle> findByBoardArticleId(@RequestParam String boardArticleId);
	
	@PutMapping
	BoardArticle modify(@RequestBody BoardArticle boardArticle);
	
	@DeleteMapping
	void delete(@RequestBody BoardArticle boardArticle);

}