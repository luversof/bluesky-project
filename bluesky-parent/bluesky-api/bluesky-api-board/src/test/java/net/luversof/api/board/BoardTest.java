package net.luversof.api.board;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.luversof.boot.jdbc.datasource.context.RoutingDataSourceContextHolder;
import net.luversof.GeneralTest;
import net.luversof.api.board.domain.Board;
import net.luversof.api.board.service.BoardService;

class BoardTest implements GeneralTest {

  private static final Logger log = LoggerFactory.getLogger(BoardTest.class);

  @Autowired private BoardService boardService;

  @BeforeAll
  static void beforeAll() {
    RoutingDataSourceContextHolder.setContext(() -> "board_postgresql");
  }

  @Test
  void create() {
    Board board = new Board();
    board.setAlias("free");

    // var jsonConfig = Map.of("key1", "value1", "key2", "value2");
    var jsonConfig = new HashMap<String, Object>();
    jsonConfig.put("key1", "value1");
    jsonConfig.put("key2", "value2");
    board.setJsonConfig(jsonConfig);

    boardService.create(board);
    log.debug("board : {}", board);
  }

  // 1407은 되고 1408은 안되네;;
  int bitIndex = 1407;

  @Test
  void update() {
    Board board = boardService.findByAlias("free");

    if (board.getJsonConfig() == null) {
      Map<String, Object> jsonConfig = Map.of("key3", "value3");
      board.setJsonConfig(jsonConfig);
    } else {
      // board.getJsonConfig().put("key3", "value3");
      // board.getJsonConfig().setKey3("key3");
    }

    var result = boardService.update(board);
    assertThat(result).isNotNull();
    log.debug("jsonConfig : {}", result.getJsonConfig());
  }

  @Test
  void read() {
    var board = boardService.findByAlias("free");
    log.debug("board : {}", board);
  }

  @Test
  void test2() {
    Board board = boardService.findByAlias("free");
    log.debug("board : {}", board);
    Board board2 = boardService.findByAlias("free2");
    log.debug("board2 : {}", board2);
  }
}
