package net.luversof.web.gate.board.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.web.gate.board.client.BoardClient;
import net.luversof.web.gate.board.domain.Board;

@RestController
@RequestMapping(value = "/api/board", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardController {
	
	@Autowired
	private BoardClient boardClient;

	@GetMapping("/findByAlias")
	public Board findByAlias(@RequestParam String alias) {
		return boardClient.findByAlias(alias);
	}

}