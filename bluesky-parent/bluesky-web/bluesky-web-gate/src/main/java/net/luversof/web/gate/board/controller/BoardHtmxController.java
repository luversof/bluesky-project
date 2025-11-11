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
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.board.openfeign.BoardArticleClient;

@Controller
@RequestMapping(value = "/board/htmx", produces = MediaType.TEXT_HTML_VALUE)
@HtmxResponseHeader("#{boardMode}HtmxResponseTrigger")
public class BoardHtmxController {

	@Setter(onMethod_ = @Autowired)
	private BoardArticleClient boardArticleClient;

	@GetMapping("/{boardAlias}/{boardMode:list}")
	public String boardArticlePage(@PathVariable String boardAlias, @PathVariable String boardMode, Pageable pageable,
			Model model) {
		var page = boardArticleClient.findByBoardAlias(boardAlias, pageable);

		// 사용자 정보 조회하여 username 추가
		var userIds = page.getContent().stream()
				.map(BoardArticle::userId)
				.distinct()
				.toList();

		var usernames = net.luversof.client.user.util.UserUtil.getUsernames(userIds);

		var enrichedContent = page.getContent().stream()
				.map(article -> article.toBuilder()
						.username(usernames.getOrDefault(article.userId(), "알 수 없음"))
						.build())
				.toList();

		model.addAttribute("page", page);
		model.addAttribute("enrichedContent", enrichedContent);
		return "board/htmx/list";
	}

	// @PostMapping("/{boardAlias}/{boardMode:write}")
	// @ResponseBody
	// public String write(@PathVariable String boardMode) {
	// return "board/htmx/write";
	// }
}
