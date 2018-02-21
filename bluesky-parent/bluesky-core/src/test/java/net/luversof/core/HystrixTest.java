package net.luversof.core;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

@Slf4j
public class HystrixTest extends GeneralTest {

	@Autowired
	private HystrixTestService hystrixTestService;
	
	@Test
	public void test() {
		log.debug("hystrixTestService : {}", hystrixTestService.getStores("testaaa"));
	}
}
