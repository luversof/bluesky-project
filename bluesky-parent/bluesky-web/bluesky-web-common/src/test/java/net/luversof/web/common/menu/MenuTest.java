package net.luversof.web.common.menu;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;
import net.luversof.web.common.menu.domain.Menu;

@Slf4j
public class MenuTest {
	
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
