package net.luversof.api.board;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.board.constant.BoardBitConfig;
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
		board.setBitConfig(BoardBitConfig.getBitConfig());
		boardService.create(board);
		log.debug("board : {}", board);
	}
	
	// 1407은 되고 1408은 안되네;;
	int bitIndex = 1407;
	
	@Test
	void update() {
		Board board = boardService.findByAlias("free");
		board.getBitConfig().set(bitIndex, true);
		board.setBitConfig(BoardBitConfig.getBitConfig());
		var result = boardService.update(board);
		assertThat(result).isNotNull();
		log.debug("bitIndex : {}", result.getBitConfig().get(bitIndex));
	}
	
	@Test
	void read() {
		var board = boardService.findByAlias("free");
		log.debug("bitIndex : {}", board.getBitConfig().get(bitIndex));
		log.debug("bitIndex : {}", board.getBitConfig().get(bitIndex - 10));
		log.debug("bitIndex : {}", board.getBitConfig());
	}
	
	@Test
	void test2() {
		Board board = boardService.findByAlias("free");
		log.debug("board : {}", board);
		log.debug("board config : {}", board.getBitConfig());
		log.debug("board config 1 : {}", board.getBitConfig().get(1));
		log.debug("board config 2 : {}", board.getBitConfig().get(2));
		log.debug("board config 3 : {}", board.getBitConfig().get(3));
		log.debug("board config 4 : {}", board.getBitConfig().get(4));
		Board board2 = boardService.findByAlias("free2");
		log.debug("board2 : {}", board2);
	}
}
