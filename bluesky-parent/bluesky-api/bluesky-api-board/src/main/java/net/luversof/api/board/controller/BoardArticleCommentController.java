package net.luversof.api.board.controller;

import java.util.UUID;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import net.luversof.api.board.domain.BoardArticleComment;
import net.luversof.api.board.service.BoardArticleCommentService;
import net.luversof.api.board.domain.BoardArticleCommentCount;

@RestController
@RequestMapping(value = "/api/boardArticleComment", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardArticleCommentController {

	@Setter(onMethod_ = @Autowired)
	private BoardArticleCommentService boardArticleCommentService;

	@PostMapping
	public BoardArticleComment create(
			@Validated(BoardArticleComment.Create.class) @RequestBody BoardArticleComment boardArticleComment) {
		return boardArticleCommentService.save(boardArticleComment);
	}

	@GetMapping("/search/findByBoardArticleId/{boardArticleId}")
	public Page<BoardArticleComment> findByBoardArticleId(@PathVariable UUID boardArticleId,
			@PageableDefault(size = 10) @SortDefault(sort = "createdDate", direction = Direction.DESC) Pageable pageable) {
		return boardArticleCommentService.findByBoardArticleId(boardArticleId, pageable);
	}

	@GetMapping("/search/countByBoardArticleId/{boardArticleId}")
	public long countByBoardArticleId(@PathVariable UUID boardArticleId) {
		return boardArticleCommentService.countByBoardArticleId(boardArticleId);
	}

	@PostMapping("/search/countByBoardArticleIds")
	public List<BoardArticleCommentCount> countByBoardArticleIds(@RequestBody List<UUID> boardArticleIds) {
		return boardArticleCommentService.countByBoardArticleIds(boardArticleIds);
	}

	@PutMapping
	public BoardArticleComment modify(
			@Validated(BoardArticleComment.Modify.class) @RequestBody BoardArticleComment boardArticleComment) {
		return boardArticleCommentService.update(boardArticleComment);
	}

	@DeleteMapping
	public void delete(
			@Validated(BoardArticleComment.Delete.class) @RequestBody BoardArticleComment boardArticleComment) {
		boardArticleCommentService.delete(boardArticleComment);
	}
}
