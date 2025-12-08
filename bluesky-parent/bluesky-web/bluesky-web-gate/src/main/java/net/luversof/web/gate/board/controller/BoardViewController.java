package net.luversof.web.gate.board.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.exception.BlueskyException;
import lombok.Setter;
import net.luversof.web.gate.board.domain.Board;
import net.luversof.web.gate.board.httpexchange.BoardArticleClient;
import net.luversof.web.gate.board.httpexchange.BoardClient;
import net.luversof.web.gate.board.service.BoardUserInfoService;
import net.luversof.web.gate.util.UserUtil;

@Controller
@RequestMapping(value = "/board", produces = MediaType.TEXT_HTML_VALUE)
public class BoardViewController {

	@Setter(onMethod_ = @Autowired)
	private BoardClient boardClient;

	@Setter(onMethod_ = @Autowired)
	private BoardArticleClient boardArticleClient;

	@Setter(onMethod_ = @Autowired)
	private BoardUserInfoService boardUserInfoService;

	@GetMapping
	public String index() {
		return "board/index";
	}

	@GetMapping("/{boardAlias}/{boardMode:list}")
	public String list(@PathVariable String boardAlias, @PathVariable String boardMode, Model model) {
		var board = checkBoard(boardAlias);
		model.addAttribute("board", board);
		return "board/list";
	}

	@GetMapping("/{boardAlias}/{boardMode:view}")
	public String view(@PathVariable String boardAlias, @PathVariable String boardMode,
			@RequestParam UUID boardArticleId, Model model) {
		var board = checkBoard(boardAlias);
		model.addAttribute("board", board);
		model.addAttribute("boardArticleId", boardArticleId);

		var boardArticle = boardArticleClient.findById(boardArticleId)
				.orElseThrow(() -> new BlueskyException("board.NOT_EXIST_BOARD_ARTICLE"));

		model.addAttribute("boardArticle", boardUserInfoService.enrich(boardArticle));

		// 현재 로그인한 사용자가 작성자인지 확인
		UUID currentUserId = UserUtil.getUserId();
		boolean isOwner = currentUserId != null && currentUserId.equals(boardArticle.userId());
		model.addAttribute("isOwner", isOwner);
		model.addAttribute("isAuthenticated", currentUserId != null);
		model.addAttribute("currentUserId", currentUserId);

		return "board/view";
	}

	@GetMapping("/{boardAlias}/{boardMode:write}")
	public String write(@PathVariable String boardAlias, @PathVariable String boardMode, Model model) {
		var board = checkBoard(boardAlias);
		model.addAttribute("board", board);
		return "board/write";
	}

	@GetMapping("/{boardAlias}/{boardMode:modify}")
	public String modify(@PathVariable String boardAlias, @PathVariable String boardMode,
			@RequestParam UUID boardArticleId, Model model) {
		var board = checkBoard(boardAlias);
		model.addAttribute("board", board);

		var boardArticle = boardArticleClient.findById(boardArticleId)
				.orElseThrow(() -> new BlueskyException("board.NOT_EXIST_BOARD_ARTICLE"));
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
