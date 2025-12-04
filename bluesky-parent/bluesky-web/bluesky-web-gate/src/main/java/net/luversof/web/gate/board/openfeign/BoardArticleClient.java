//package net.luversof.web.gate.board.openfeign;
//
//import java.util.Optional;
//import java.util.UUID;
//
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//
//import net.luversof.web.gate.board.domain.BoardArticle;
//
///**
// * 게시글 관련 API를 호출하는 OpenFeign 클라이언트 (bluesky-api-board 호출)
// */
//@FeignClient(name = "bluesky-api-board", contextId = "api-board-boardArticle", path = "/api/boardArticle", url = "${gate.feign-client.url.board:}")
//public interface BoardArticleClient {
//
//	/**
//	 * 새 게시글 작성
//	 */
//	@PostMapping
//	BoardArticle create(@RequestBody BoardArticle boardArticle);
//
//	/**
//	 * 게시판 alias로 게시글 목록 조회 (페이지네이션)
//	 */
//	@GetMapping("/search/findByBoardAlias/{boardAlias}")
//	Page<BoardArticle> findByBoardAlias(@PathVariable String boardAlias, Pageable pageable);
//
//	/**
//	 * ID로 특정 게시글 조회
//	 */
//	@GetMapping("/{id}")
//	Optional<BoardArticle> findById(@PathVariable UUID id);
//
//	/**
//	 * 게시글 수정
//	 */
//	@PutMapping
//	BoardArticle modify(@RequestBody BoardArticle boardArticle);
//
//	/**
//	 * 게시글 삭제
//	 */
//	@DeleteMapping
//	void delete(@RequestBody BoardArticle boardArticle);
//}