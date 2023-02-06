package net.luversof.api.board;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.board.domain.Board;
import net.luversof.api.board.service.BoardService;

@Slf4j
class BoardTest implements GeneralTest {

	@Autowired
	private BoardService boardService;
	
	@Test
	void create() {
		Board board = new Board();
		board.setAlias("free");
		boardService.create(board);
		log.debug("board : {}", board);
	}
	
	@Test
	void update() {
		Board board = boardService.findByAlias("free");
		board.setBoardActivated(true);
		boardService.update(board);
	}
	
	@Test
	void test2() {
		Board board = boardService.findByAlias("free");
		log.debug("board : {}", board);
		Board board2 = boardService.findByAlias("free2");
		log.debug("board2 : {}", board2);
	}
}
