package net.luversof.web.gate.board.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.board.domain.Board;

/**
 * 게시판 관련 API를 호출하는 OpenFeign 클라이언트 (bluesky-api-board 호출)
 */
@FeignClient(name = "bluesky-api-board", contextId="api-board-board", path = "/api/board", url = "${gate.feign-client.url.board:}")
public interface BoardClient {

	/**
	 * 새 게시판 생성
	 */
	@PostMapping
	Board create(@RequestBody Board board);

	/**
	 * 게시판 alias로 조회
	 */
	@GetMapping("/findByAlias")
	Board findByAlias(@RequestParam String alias);

	/**
	 * 모든 게시판 목록 조회
	 */
	@GetMapping("/findAll")
	Iterable<Board> findAll();

	/**
	 * 게시판 정보 수정
	 */
	@PutMapping
	Board update(@RequestBody Board board);
}