package net.luversof.web.gate.board;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralWebTest;
import net.luversof.web.gate.board.client.BoardArticleClient;
import net.luversof.web.gate.board.client.BoardClient;
import net.luversof.web.gate.board.domain.BoardArticle;

@Slf4j
class BoardArticleTest implements GeneralWebTest {
	
	@Autowired
	private BoardClient boardClient;
	
	@Autowired
	private BoardArticleClient boardArticleClient;
	
	@Test
	@DisplayName("게시글 생성")
	void create() {
		var board = boardClient.findByAlias("free");
		var boardArticle = new BoardArticle();
		boardArticle.setBoardId(board.boardId());
		boardArticle.setUserId("userId");
		boardArticle.setTitle("title");
		boardArticle.setContent("content");
		var resultBoardArticle = boardArticleClient.create(boardArticle);
		assertThat(resultBoardArticle).isNotNull();
	}
	
	@Test
	@DisplayName("게시글 목록 조회")
	void findByBoardAlias() {
		var pageRequest =  PageRequest.of(0, 20).withSort(Sort.by(Order.desc("id"), Order.asc("boardId")));
		var boardArticlePage = boardArticleClient.findByBoardAlias("free", pageRequest);
		log.debug("boardArticlePage.getContent() : {}", boardArticlePage.getContent());
		assertThat(boardArticlePage).isNotEmpty();
	}
	
	@Test
	void findByBoardArticleId() {
		var boardArticle = boardArticleClient.findByBoardArticleId("208e94d0-2560-4517-8ff5-1892d9f5f4df");
		log.debug("boardArticle : {}", boardArticle);
		assertThat(boardArticle).isNotNull();
	}
	
	@Test
	void a() {
		Sort sort = Sort.by("id").descending();
		log.debug("sort : ", sort.toList());
	}

}
