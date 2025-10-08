package net.luversof.api.board.controller;

import java.util.Optional;
import java.util.UUID;

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

import net.luversof.api.board.domain.BoardArticle;
import net.luversof.api.board.service.BoardArticleService;

@RestController
@RequestMapping(value = "/api/boardArticle", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardArticleController {

	@Autowired
	private BoardArticleService boardArticleService;

	@PostMapping
	public BoardArticle create(@Validated(BoardArticle.Create.class) @RequestBody BoardArticle boardArticle) {
		return boardArticleService.save(boardArticle);
	}

	@GetMapping("/findByBoardAlias/{boardAlias}")
	public Page<BoardArticle> findByBoardAlias(@PathVariable String boardAlias, @PageableDefault(size = 20) @SortDefault(sort = "id", direction = Direction.DESC) Pageable pageable) {
		return boardArticleService.findByAlias(boardAlias, pageable);
	}

	@GetMapping("/{id}")
	public Optional<BoardArticle> findByBoardArticleId(@PathVariable UUID id) {
		return boardArticleService.findById(id);
	}

	@PutMapping
	public BoardArticle modify(@Validated(BoardArticle.Modify.class) @RequestBody BoardArticle boardArticle) {
		return boardArticleService.update(boardArticle);
	}

	@DeleteMapping
	public void delete(@Validated(BoardArticle.Delete.class) @RequestBody BoardArticle boardArticle) {
		boardArticleService.delete(boardArticle);
	}
}
