package net.luversof.web.gate.board.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import net.luversof.web.gate.board.domain.Board;
import net.luversof.web.gate.board.openfeign.BoardClient;

@RestController
@RequestMapping(value = "/api/board", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardApiController {
	
	@Setter(onMethod_ = @Autowired)
	private BoardClient boardClient;

	@GetMapping("/search/findByAlias/{alias}")
	public Board findByAlias(@PathVariable String alias) {
		return boardClient.findByAlias(alias);
	}

}