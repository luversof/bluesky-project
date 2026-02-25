package net.luversof.web.gate.board.controller;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.board.httpexchange.BoardArticleClient;
import net.luversof.web.gate.board.service.BoardUserInfoService;

@RestController
@RequestMapping(value = "/api/boardArticle", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardArticleApiController {

	private static final Logger log = LoggerFactory.getLogger(BoardArticleApiController.class);

	private BoardArticleClient boardArticleClient;

	private BoardUserInfoService boardUserInfoService;

	@Autowired
	public void setBoardArticleClient(BoardArticleClient boardArticleClient) {
		this.boardArticleClient = boardArticleClient;
	}

	@Autowired
	public void setBoardUserInfoService(BoardUserInfoService boardUserInfoService) {
		this.boardUserInfoService = boardUserInfoService;
	}

	@BlueskyPreAuthorize
	@PostMapping
	public BoardArticle create(@RequestBody BoardArticle boardArticle) {
		var createdArticle = boardArticleClient.create(boardArticle.toBuilder().userId(UserUtil.getUserId()).build());
		return boardUserInfoService.enrich(createdArticle);
	}

	/**
	 * Sort를 query parameter로 변경하기 귀찮아서 매개변수 처리하지 않음
	 * 
	 * @param boardAlias
	 * @param page
	 * @param pageable
	 * @return
	 */
	@GetMapping("/search/findByBoardAlias/{boardAlias}")
	public Page<BoardArticle> findByBoardAlias(@PathVariable String boardAlias,
			@PageableDefault(size = 20) @SortDefault(sort = "createdDate", direction = Direction.DESC) Pageable pageable) {
		log.debug("findByBoardAlias boardAlias : {}", boardAlias);
		var page = boardArticleClient.findByBoardAlias(boardAlias, pageable);
		return boardUserInfoService.enrich(page).toPage();
	}

	@GetMapping("/{id}")
	public Optional<BoardArticle> findById(@PathVariable UUID id) {
		return boardArticleClient.findById(id).map(boardUserInfoService::enrich);
	}

	@BlueskyPreAuthorize
	@PutMapping
	public BoardArticle modify(@RequestBody BoardArticle boardArticle) {
		var updatedArticle = boardArticleClient.modify(boardArticle.toBuilder().userId(UserUtil.getUserId()).build());
		return boardUserInfoService.enrich(updatedArticle);
	}

	@BlueskyPreAuthorize
	@DeleteMapping
	public void delete(@RequestBody BoardArticle boardArticle) {
		boardArticleClient.delete(boardArticle.toBuilder().userId(UserUtil.getUserId()).build());
	}
}
