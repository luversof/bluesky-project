package net.luversof.web.gate.board;

import static org.assertj.core.api.Assertions.assertThat;

import net.luversof.GeneralWebTest;
import net.luversof.web.gate.board.httpexchange.BoardClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BoardTest implements GeneralWebTest {

    @Autowired private BoardClient boardClient;

    @Test
    void findByAlias() {
        var board = boardClient.findByAlias("free");
        assertThat(board).isNotNull();
    }
}
