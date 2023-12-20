package net.luversof.web.dynamiccrud.setting;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;


@Slf4j
class SimpleTest {

	@Test
	void stringSplitTest() {
		String a = "select * from test";
		
		
		log.debug("result : {}", a.split("FROM").length);
		log.debug("result : {}", a.split("from").length);
		log.debug("result : {}", Pattern.compile(" FROM ", Pattern.CASE_INSENSITIVE).split(a).length);
		log.debug("result : {}", Pattern.compile(" fRom ", Pattern.CASE_INSENSITIVE).split(a).length);
	}
	
}
