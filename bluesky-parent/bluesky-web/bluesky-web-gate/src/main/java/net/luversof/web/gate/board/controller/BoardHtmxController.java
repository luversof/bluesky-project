package net.luversof.web.gate.board.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.luversof.boot.htmx.annotation.HtmxResponseHeader;
import net.luversof.web.gate.board.httpexchange.BoardArticleClient;
import net.luversof.web.gate.board.service.BoardUserInfoService;

@Controller
@RequestMapping(value = "/board/htmx", produces = MediaType.TEXT_HTML_VALUE)
@HtmxResponseHeader("#{boardMode}HtmxResponseTrigger")
public class BoardHtmxController {

	private BoardArticleClient boardArticleClient;

	private BoardUserInfoService boardUserInfoService;

	@Autowired
	public void setBoardArticleClient(BoardArticleClient boardArticleClient) {
		this.boardArticleClient = boardArticleClient;
	}

	@Autowired
	public void setBoardUserInfoService(BoardUserInfoService boardUserInfoService) {
		this.boardUserInfoService = boardUserInfoService;
	}

	@GetMapping("/{boardAlias}/{boardMode:list}")
	public String boardArticlePage(@PathVariable String boardAlias, @PathVariable String boardMode, Pageable pageable,
			Model model) {
		var pageResponse = boardUserInfoService.enrich(boardArticleClient.findByBoardAlias(boardAlias, pageable));
		model.addAttribute("page", pageResponse.toPage());
		return "board/htmx/list";
	}

}
