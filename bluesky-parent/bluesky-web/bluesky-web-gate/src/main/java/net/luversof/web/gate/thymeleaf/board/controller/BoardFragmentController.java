package net.luversof.web.gate.thymeleaf.board.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletResponse;
import static net.luversof.web.gate.constant.GateConstant.HX_TRIGGER;
import net.luversof.web.gate.feign.board.client.BoardArticleClient;
import net.luversof.web.gate.feign.board.domain.BoardArticle;

@Controller
@RequestMapping(value = "/board/fragment", produces = MediaType.TEXT_HTML_VALUE)
public class BoardFragmentController {
	
	@Autowired
	private BoardArticleClient boardArticleClient;

	@GetMapping("/list")
	public Page<BoardArticle> boardArticlePage(String boardAlias, Pageable pageable, HttpServletResponse response) {
		response.setHeader(HX_TRIGGER, "listFragmentResponseTrigger");
		return boardArticleClient.findByBoardAlias(boardAlias, pageable);
	}

}
