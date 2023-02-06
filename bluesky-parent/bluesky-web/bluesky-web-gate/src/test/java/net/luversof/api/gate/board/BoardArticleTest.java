package net.luversof.api.gate.board;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralTest;
import net.luversof.web.gate.board.client.BoardArticleClient;

class BoardArticleTest implements GeneralTest {
	
	@Autowired
	private BoardArticleClient boardArticleClient;
	
	@Test
	void create() {
		
		
		boardArticleClient.create(null);
	}

}
