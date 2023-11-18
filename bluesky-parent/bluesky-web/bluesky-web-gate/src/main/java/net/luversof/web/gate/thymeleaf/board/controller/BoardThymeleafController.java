package net.luversof.web.gate.thymeleaf.board.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.gate.feign.board.client.BoardClient;

@Controller
@RequestMapping(value = "/board", produces = MediaType.TEXT_HTML_VALUE)
public class BoardThymeleafController {
	
	@Autowired
	private BoardClient boardClient;

	@GetMapping 
	public String index() {
		return "board/index";
	}
	
	@GetMapping("/{boardAlias}/list")
	public String list(@PathVariable String boardAlias, Model model)	{
		var board = boardClient.findByAlias(boardAlias);
		if (board == null) {
			throw new BlueskyException("board.NOT_EXIST_BOARD");
		}
		model.addAttribute(board);
		return "board/list";
	}
}
