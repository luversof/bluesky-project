package net.luversof.web.gate.thymeleaf.board.controller;

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
import net.luversof.web.gate.feign.board.client.BoardArticleClient;
import net.luversof.web.gate.feign.board.domain.BoardArticle;

@Controller
@RequestMapping(value = "/board/fragment", produces = MediaType.TEXT_HTML_VALUE)
@HtmxResponseHeader("#{boardMode}FragmentResponseTrigger")
public class BoardFragmentController {
	
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
