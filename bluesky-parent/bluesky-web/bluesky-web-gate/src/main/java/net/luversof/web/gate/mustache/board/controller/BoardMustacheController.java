package net.luversof.web.gate.mustache.board.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.web.gate.board.client.BoardArticleClient;
import net.luversof.web.gate.board.domain.BoardArticle;

@Controller
@RequestMapping(value = "/parts/board", produces = "text/mustache" )
public class BoardMustacheController {
	
	@Autowired
	private BoardArticleClient boardArticleClient;

	@GetMapping("/boardArticlePage")
	public Page<BoardArticle> boardArticlePage(String boardAlias, Pageable pageable) {
		return boardArticleClient.findByBoardAlias(boardAlias, pageable);
	}
	
	@GetMapping("/test")
	public String test(Model model) {
		model.addAttribute("test" , "테스트값");
		return "board/list.parts";
	}
}
