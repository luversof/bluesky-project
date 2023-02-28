package net.luversof.api.board.controller;

import java.util.Optional;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.board.domain.BoardArticle;
import net.luversof.api.board.service.BoardArticleService;

@RestController
@RequestMapping(value = "/api/board/article", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardArticleController {

	@Autowired
	private BoardArticleService boardArticleService;

	@PostMapping
	public BoardArticle create(@Validated(BoardArticle.Create.class) @RequestBody BoardArticle boardArticle) {
		return boardArticleService.create(boardArticle);
	}

	@GetMapping("/findByBoardAlias")
	public Page<BoardArticle> findByBoardAlias(@RequestParam String boardAlias,
			@PageableDefault(size = 20) @SortDefault(sort = "id", direction = Direction.DESC) Pageable pageable) {
		return boardArticleService.findByAlias(boardAlias, pageable);
	}

	@GetMapping("/findByBoardArticleId")
	public Optional<BoardArticle> findByBoardArticleId(@RequestParam String boardArticleId) {
		return boardArticleService.findByBoardArticleId(boardArticleId);
	}

	@PutMapping
	public BoardArticle modify(@Validated(BoardArticle.Modify.class) @RequestBody BoardArticle boardArticle) {
		return boardArticleService.modify(boardArticle);
	}

	@DeleteMapping
	public void delete(@Validated(BoardArticle.Delete.class) @RequestBody BoardArticle boardArticle) {
		boardArticleService.delete(boardArticle);
	}
}
