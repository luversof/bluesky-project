package net.luversof.web.common.menu;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.luversof.web.common.menu.domain.Menu;

public class MenuTest {

	private static final Logger log = LoggerFactory.getLogger(MenuTest.class);

	@Test
	void patternTest() {
		var url = "/board/free/list";
		log.debug("result : {}", Pattern.compile("\\/board\\/.*").matcher(url).matches());
	}

	@Test
	void menuTest() {
		var menu = new Menu();
		menu.setUrl("/board/free/list");
		log.debug("test : {}", "A");
	}
}
