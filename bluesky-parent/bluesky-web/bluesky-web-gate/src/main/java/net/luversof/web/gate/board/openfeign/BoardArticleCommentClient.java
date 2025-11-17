package net.luversof.web.gate.board.openfeign;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import net.luversof.web.gate.board.domain.BoardArticleComment;

@FeignClient(name = "bluesky-api-board", contextId = "api-board-boardArticleComment", path = "/api/boardArticleComment", url = "${gate.feign-client.url.board:}")
public interface BoardArticleCommentClient {

	@PostMapping
	BoardArticleComment create(@RequestBody BoardArticleComment boardArticleComment);

	@GetMapping("/search/findByBoardArticleId/{boardArticleId}")
	Page<BoardArticleComment> findByBoardArticleId(@PathVariable UUID boardArticleId, Pageable pageable);

	@GetMapping("/search/countByBoardArticleId/{boardArticleId}")
	long countByBoardArticleId(@PathVariable UUID boardArticleId);

	@PutMapping
	BoardArticleComment modify(@RequestBody BoardArticleComment boardArticleComment);

	@DeleteMapping
	void delete(@RequestBody BoardArticleComment boardArticleComment);
}
