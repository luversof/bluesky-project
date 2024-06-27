package net.luversof.api.board;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.board.domain.Board;
import net.luversof.api.board.domain.BoardArticle;
import net.luversof.api.board.repository.BoardArticleRepository;
import net.luversof.api.board.service.BoardArticleService;
import net.luversof.api.board.service.BoardService;

@Slf4j
@Transactional
@Rollback(false)
class BoardArticleTest implements GeneralTest {

	@Autowired
	private BoardArticleRepository boardArticleRepository;
	
	@Autowired
	private BoardService boardService;
	
	@Autowired
	private BoardArticleService boardArticleService;
	
	@Test
	@DisplayName("게시글 생성")
	void create() {
		
		Board board = boardService.findByAlias("free");
		BoardArticle boardArticle = new BoardArticle();
		boardArticle.setBoardId(board.getId());
		boardArticle.setUserId("1");
		boardArticle.setTitle("테스트");
		boardArticle.setContent("내용");
		boardArticleService.create(boardArticle);
	}
	
	@RepeatedTest(value = 10000, name = "반복 입력")
	void 다량입력(RepetitionInfo repetitionInfo) {
		Board board = boardService.findByAlias("free");
		
		int i = repetitionInfo.getCurrentRepetition();

//		List<BoardArticle> boardArticleList = new ArrayList<>();
//		for (int i = 0 ; i < 100000 ; i ++) {
			BoardArticle boardArticle = new BoardArticle();
			boardArticle.setBoardId(board.getId());
			boardArticle.setUserId("1");
			boardArticle.setTitle("테스트" + i);
			boardArticle.setContent("내용" + i);
//			boardArticleList.add(boardArticle);
//		}
		boardArticleRepository.save(boardArticle);
//		boardArticleRepository.saveAll(boardArticleList);
	}
	
	@Test
	@DisplayName("게시글 목록 조회")
	void findByAlias() {
		var boardArticlePage = boardArticleService.findByAlias("free", PageRequest.of(0, 20, Sort.by("id").descending()));
		assertThat(boardArticlePage).isNotEmpty();
	}
	
	@Test
	@DisplayName("게시글 조회")
	void findByBoardArticleId() {
		var boardArticleOptional = boardArticleRepository.findById(UUID.fromString("4a699a6e-dfb7-4749-b0c4-eb969714e59f"));
		assertThat(boardArticleOptional).isNotEmpty();
	}
	
	@Test
	void 페이징테스트() {
		Board board = boardService.findByAlias("free");
		Page<BoardArticle> boardArticleList = boardArticleRepository.findByBoardId(board.getId(), PageRequest.of(0, 20, Sort.by("id").descending()));
		log.debug("result : {}", boardArticleList.getContent());
		
	}
}
