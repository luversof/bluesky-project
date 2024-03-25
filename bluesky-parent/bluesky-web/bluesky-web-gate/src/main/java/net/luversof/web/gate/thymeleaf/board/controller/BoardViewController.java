package net.luversof.web.gate.thymeleaf.board.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.gate.feign.board.client.BoardArticleClient;
import net.luversof.web.gate.feign.board.client.BoardClient;
import net.luversof.web.gate.feign.board.domain.Board;

@Controller
@RequestMapping(value = "/board", produces = MediaType.TEXT_HTML_VALUE)
public class BoardViewController {
	
	@Autowired
	private BoardClient boardClient;
	
	@Autowired
	private BoardArticleClient boardArticleClient;

	@GetMapping 
	public String index() {
		return "board/index";
	}
	
	@GetMapping("/{boardAlias}/{boardMode:list}")
	public String list(@PathVariable String boardAlias, @PathVariable String boardMode, Model model)	{
		var board = checkBoard(boardAlias);
		model.addAttribute(board);
		return "board/list";
	}
	
	@GetMapping("/{boardAlias}/{boardMode:view}")
	public String view(@PathVariable String boardAlias, @PathVariable String boardMode, @RequestParam String boardArticleId, Model model)	{
		var board = checkBoard(boardAlias);
		model.addAttribute(board);
		
		var boardArticle = boardArticleClient.findByBoardArticleId(boardArticleId).orElseThrow(() -> new BlueskyException("board.NOT_EXIST_BOARD_ARTICLE"));
		model.addAttribute(boardArticle);
		
		return "board/view";
	}
	
	@GetMapping("/{boardAlias}/{boardMode:write}")
	public String write(@PathVariable String boardAlias, @PathVariable String boardMode, Model model)	{
		var board = checkBoard(boardAlias);
		model.addAttribute(board);
		return "board/write";
	}
	
	@GetMapping("/{boardAlias}/{boardMode:modify}")
	public String modify(@PathVariable String boardAlias, @PathVariable String boardMode, @RequestParam String boardArticleId, Model model)	{
		var board = checkBoard(boardAlias);
		model.addAttribute(board);
		
		var boardArticle = boardArticleClient.findByBoardArticleId(boardArticleId).orElseThrow(() -> new BlueskyException("board.NOT_EXIST_BOARD_ARTICLE"));
		model.addAttribute(boardArticle);
		return "board/modify";
	}
	
	
	private Board checkBoard(String boardAlias) {
		var board = boardClient.findByAlias(boardAlias);
		if (board == null) {
			throw new BlueskyException("board.NOT_EXIST_BOARD");
		}
		return board;
	}
}
