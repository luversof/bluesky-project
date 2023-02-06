package net.luversof.api.gate.board;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralTest;
import net.luversof.web.gate.board.client.BoardClient;

class BoardTest implements GeneralTest {
	
	@Autowired
	private BoardClient boardClient;

	@Test
	void findByAlias() {
		boardClient.findByAlias("free");
	}
}
