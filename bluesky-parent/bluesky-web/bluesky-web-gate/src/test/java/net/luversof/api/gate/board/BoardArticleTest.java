package net.luversof.api.gate.board;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralWebTest;
import net.luversof.web.gate.board.client.BoardArticleClient;
import net.luversof.web.gate.board.client.BoardClient;
import net.luversof.web.gate.board.domain.BoardArticle;

class BoardArticleTest implements GeneralWebTest {
	
	@Autowired
	private BoardClient boardClient;
	
	@Autowired
	private BoardArticleClient boardArticleClient;
	
	@Test
	void create() {
		var board = boardClient.findByAlias("free");
		var boardArticle = BoardArticle.builder()
				.boardId(board.boardId())
				.userId("userId")
				.title("title")
				.content("content")
				.build();
		var resultBoardArticle = boardArticleClient.create(boardArticle);
		assertThat(resultBoardArticle).isNotNull();
	}

}
