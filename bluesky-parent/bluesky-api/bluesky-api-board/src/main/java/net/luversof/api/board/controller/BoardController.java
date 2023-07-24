package net.luversof.api.board.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import net.luversof.api.board.controller.swagger.BoardControllerOperation;
import net.luversof.api.board.domain.Board;
import net.luversof.api.board.service.BoardService;

@Slf4j
@RestController
@RequestMapping(value = "/api/board", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardController {
	
	@Autowired
	private BoardService boardService;

	@PostMapping
	@BoardControllerOperation.Create
	public Board create(@RequestBody Board board) {
		return boardService.create(board);
	}
	
	@GetMapping("/findByAlias")
	public Board findByAlias(@RequestParam String alias) {
		var a =  boardService.findByAlias(alias);
		log.debug("test : {}", a);
//		var b = boardService.findByAlias2(alias);
//		log.debug("test222 : {}", b);
		return a;
	}
	
	@PutMapping
	public Board update(@RequestBody Board board) {
		return boardService.update(board);
	}

}
