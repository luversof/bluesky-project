package net.luversof.web.gate.board.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import lombok.extern.slf4j.Slf4j;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.board.openfeign.BoardArticleClient;

@Slf4j
@RestController
@RequestMapping(value = "/json/boardArticle", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardArticleJsonController {

	@Autowired
	private BoardArticleClient boardArticleClient;
	
	@BlueskyPreAuthorize
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
	public Page<BoardArticle> findByBoardAlias(@RequestParam String boardAlias, @PageableDefault(size = 20) @SortDefault(sort = "id", direction = Direction.DESC) Pageable pageable) {
		log.debug("findByBoardAlias boardAlias : {}", boardAlias);
		return boardArticleClient.findByBoardAlias(boardAlias, pageable);
	}
	
	@GetMapping("/findById")
	public Optional<BoardArticle> findById(@RequestParam UUID boardArticleId) {
		return boardArticleClient.findById(boardArticleId);
	}
	
	@BlueskyPreAuthorize
	@PutMapping
	public BoardArticle modify(@RequestBody BoardArticle boardArticle) {
		return boardArticleClient.modify(boardArticle.toBuilder().userId(UserUtil.getUserId()).build());
	}
	
	@BlueskyPreAuthorize
	@DeleteMapping
	public void delete(@RequestBody BoardArticle boardArticle) {
		boardArticleClient.delete(boardArticle.toBuilder().userId(UserUtil.getUserId()).build());
	}
}
