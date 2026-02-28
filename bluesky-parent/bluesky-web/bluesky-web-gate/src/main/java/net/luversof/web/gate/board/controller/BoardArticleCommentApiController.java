package net.luversof.web.gate.board.controller;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.data.domain.PageResponse;
import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.board.domain.BoardArticleComment;
import net.luversof.web.gate.board.httpexchange.BoardArticleCommentClient;
import net.luversof.web.gate.board.service.BoardUserInfoService;

@RestController
@RequestMapping(value = "/api/boardArticleComment", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardArticleCommentApiController {

	private BoardArticleCommentClient boardArticleCommentClient;

	private BoardUserInfoService boardUserInfoService;

	@Autowired
	public void setBoardArticleCommentClient(BoardArticleCommentClient boardArticleCommentClient) {
		this.boardArticleCommentClient = boardArticleCommentClient;
	}

	@Autowired
	public void setBoardUserInfoService(BoardUserInfoService boardUserInfoService) {
		this.boardUserInfoService = boardUserInfoService;
	}

	@BlueskyPreAuthorize
	@PostMapping
	public BoardArticleComment create(@RequestBody BoardArticleComment boardArticleComment) {
		var createdComment = boardArticleCommentClient
				.create(boardArticleComment.toBuilder().userId(UserUtil.getUserId()).build());
		return boardUserInfoService.enrich(createdComment);
	}

	@GetMapping("/search/findByBoardArticleId/{boardArticleId}")
	public PageResponse<BoardArticleComment> findByBoardArticleId(@PathVariable UUID boardArticleId,
			@PageableDefault(size = 10) @SortDefault(sort = "createdDate", direction = Direction.ASC) Pageable pageable) {
		var page = boardArticleCommentClient.findByBoardArticleId(boardArticleId, pageable);
		return boardUserInfoService.enrichComments(page);
	}

	@GetMapping("/search/countByBoardArticleId/{boardArticleId}")
	public long countByBoardArticleId(@PathVariable UUID boardArticleId) {
		return boardArticleCommentClient.countByBoardArticleId(boardArticleId);
	}

	@BlueskyPreAuthorize
	@PutMapping
	public BoardArticleComment modify(@RequestBody BoardArticleComment boardArticleComment) {
		var updatedComment = boardArticleCommentClient
				.modify(boardArticleComment.toBuilder().userId(UserUtil.getUserId()).build());
		return boardUserInfoService.enrich(updatedComment);
	}

	@BlueskyPreAuthorize
	@DeleteMapping
	public void delete(@RequestBody BoardArticleComment boardArticleComment) {
		boardArticleCommentClient.delete(boardArticleComment.toBuilder().userId(UserUtil.getUserId()).build());
	}
}
