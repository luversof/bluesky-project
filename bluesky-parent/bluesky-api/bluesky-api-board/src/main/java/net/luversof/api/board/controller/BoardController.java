package net.luversof.api.board.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.luversof.api.board.controller.swagger.BoardControllerOperation;
import net.luversof.api.board.domain.Board;
import net.luversof.api.board.service.BoardService;

@Slf4j
@RestController
@RequestMapping(value = "/api/board", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardController {
	
	@Setter(onMethod_ =  @Autowired)
	private BoardService boardService;
	
	@PostMapping
	@BoardControllerOperation.Create
	public Board create(@RequestBody Board board) {
		return boardService.create(board);
	}
	
	@GetMapping("/search/findByAlias/{alias}")
	public Board findByAlias(@PathVariable String alias) {
		return boardService.findByAlias(alias);
	}
	
	@GetMapping("/search/findAll")
	public Iterable<Board> findAll() {
		return boardService.findAll();
	}
	
	@PutMapping
	public Board update(@RequestBody Board board) {
		return boardService.update(board);
	}

}
