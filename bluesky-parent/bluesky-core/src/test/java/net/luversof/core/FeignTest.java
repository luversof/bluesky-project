package net.luversof.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

@Slf4j
public class FeignTest extends GeneralTest {

	@Autowired
	private FeignTestService feignTestService;
	
	@Test
	public void test() {
		log.debug("result : {}", feignTestService.test());
	}
}
