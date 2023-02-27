package net.luversof.web.gate.board.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.web.gate.board.client.BoardArticleClient;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.user.util.UserUtil;

@RestController
@RequestMapping(value = "/api/board/article", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardArticleController {

	@Autowired
	private BoardArticleClient boardArticleClient;
	
	@PreAuthorize("hasRole('USER')")
	@PostMapping
	public BoardArticle create(@RequestBody BoardArticle boardArticle) {
		return boardArticleClient.create(boardArticle.toBuilder().userId(UserUtil.getUserId()).build());
	}
	
	/**
	 * Sort를 query parameter로 변경하기 귀찮아서 매개변수 처리하지 않음
	 * @param boardAlias
	 * @param page
	 * @param pageable
	 * @return
	 */
	@GetMapping("/findByBoardAlias")
	public Page<BoardArticle> findByBoardAlias(@RequestParam String boardAlias, @RequestParam int page) {
		return boardArticleClient.findByBoardAlias(boardAlias, page, 20, List.of("id,desc"));
	}
	
	@GetMapping("/findByBoardArticleId")
	public Optional<BoardArticle> findByBoardArticleId(@RequestParam String boardArticleId) {
		return boardArticleClient.findByBoardArticleId(boardArticleId);
	}
	
	@PreAuthorize("hasRole('USER')")
	@PutMapping
	public BoardArticle modify(@RequestBody BoardArticle boardArticle) {
		return boardArticleClient.modify(boardArticle.toBuilder().userId(UserUtil.getUserId()).build());
	}
	
	@PreAuthorize("hasRole('USER')")
	@DeleteMapping
	public void delete(@RequestParam String boardArticleId) {
		boardArticleClient.delete(BoardArticle.builder().boardArticleId(boardArticleId).userId(UserUtil.getUserId()).build());
	}
}
