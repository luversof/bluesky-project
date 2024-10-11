package net.luversof.api.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;

import java.util.Map;

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
		
//		var jsonConfig = Map.of("key1", "value1", "key2", "value2");
		var jsonConfig = new Board.JsonConfig();
		jsonConfig.setKey1("value1");
		jsonConfig.setKey2("value2");
		board.setJsonConfig(jsonConfig);
		
		boardService.create(board);
		log.debug("board : {}", board);
	}
	
	// 1407은 되고 1408은 안되네;;
	int bitIndex = 1407;
	
	@Test
	void update() {
		Board board = boardService.findByAlias("free");
		board.getBitConfig().set(bitIndex, true);
		
		if (board.getJsonConfig() == null) {
//			var jsonConfig = Map.of("key3", "value3");
			var jsonConfig = new Board.JsonConfig();
//			jsonConfig.setKey3("value3");
			board.setJsonConfig(jsonConfig);
		} else {
//			board.getJsonConfig().put("key3", "value3");
//			board.getJsonConfig().setKey3("key3");
		}
		
		var result = boardService.update(board);
		assertThat(result).isNotNull();
		log.debug("bitIndex : {}", result.getBitConfig().get(bitIndex));
		log.debug("jsonConfig : {}", result.getJsonConfig());
	}
	
	@Test
	void read() {
		var board = boardService.findByAlias("free");
		log.debug("bitIndex : {}", board.getBitConfig().get(bitIndex));
		log.debug("bitIndex : {}", board.getBitConfig().get(bitIndex - 10));
		log.debug("bitIndex : {}", board.getBitConfig());
		log.debug("jsonConfig : {}", board.getJsonConfig());
//		log.debug("jsonConfig : {}", board.getJsonConfig().get("key1"));
		log.debug("jsonConfig : {}", board.getJsonConfig().getKey1());
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
