package net.luversof.api.board;

import java.util.BitSet;

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
		var config = new BitSet();
		config.set(1);
		config.set(3);
		config.set(4);
		board.setConfig(config);
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
		log.debug("board config : {}", board.getConfig());
		log.debug("board config 1 : {}", board.getConfig().get(1));
		log.debug("board config 2 : {}", board.getConfig().get(2));
		log.debug("board config 3 : {}", board.getConfig().get(3));
		log.debug("board config 4 : {}", board.getConfig().get(4));
		Board board2 = boardService.findByAlias("free2");
		log.debug("board2 : {}", board2);
	}
}
