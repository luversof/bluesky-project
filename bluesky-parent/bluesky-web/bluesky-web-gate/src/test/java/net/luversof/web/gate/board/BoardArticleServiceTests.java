package net.luversof.web.gate.board;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralWebTest;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.board.service.BoardArticleService;

@Slf4j
public class BoardArticleServiceTests implements GeneralWebTest {
	
	@Autowired
	private BoardArticleService boardArticleService;
	
	@Test
	void test() {
		String boardAlias = "free";
		var pageRequest =  PageRequest.of(0, 20).withSort(Sort.by(Order.desc("id"), Order.asc("boardId")));
		Page<BoardArticle> boardArticlePage = boardArticleService.findByBoardAlias(boardAlias, pageRequest);
		log.debug("boardArticlePage : {}", boardArticlePage);
		assertThat(boardArticlePage).isNotNull();
	}

}
