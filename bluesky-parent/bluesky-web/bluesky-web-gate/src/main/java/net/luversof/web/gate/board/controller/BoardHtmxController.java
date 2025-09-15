package net.luversof.web.gate.board.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import io.github.luversof.boot.htmx.annotation.HtmxResponseHeader;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.board.openfeign.BoardArticleClient;

@Controller
@RequestMapping(value = "/htmx/board", produces = MediaType.TEXT_HTML_VALUE)
@HtmxResponseHeader("#{boardMode}HtmxResponseTrigger")
public class BoardHtmxController {
	
	@Autowired
	private BoardArticleClient boardArticleClient;

	@GetMapping("/{boardMode:list}")
	public Page<BoardArticle> boardArticlePage(@PathVariable String boardMode, String boardAlias, Pageable pageable) {
		return boardArticleClient.findByBoardAlias(boardAlias, pageable);
	}

	@PostMapping("/{boardMode:write}")
	@ResponseBody
	public BoardArticle write(@PathVariable String boardMode) {
		return null;
	}
}
