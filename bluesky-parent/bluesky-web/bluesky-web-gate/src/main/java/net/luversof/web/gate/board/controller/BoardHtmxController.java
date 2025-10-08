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
import lombok.Setter;
import net.luversof.web.gate.board.openfeign.BoardArticleClient;

@Controller
@RequestMapping(value = "/board/htmx", produces = MediaType.TEXT_HTML_VALUE)
@HtmxResponseHeader("#{boardMode}HtmxResponseTrigger")
public class BoardHtmxController {
	
	@Setter(onMethod_ = @Autowired)
	private BoardArticleClient boardArticleClient;

	@GetMapping("/{boardAlias}/{boardMode:list}")
	public String boardArticlePage( @PathVariable String boardAlias, @PathVariable String boardMode, Pageable pageable, Model model) {
		model.addAttribute("page", boardArticleClient.findByBoardAlias(boardAlias, pageable));
		return "board/htmx/list";
	}

//	@PostMapping("/{boardAlias}/{boardMode:write}")
//	@ResponseBody
//	public String write(@PathVariable String boardMode) {
//		return "board/htmx/write";
//	}
}
