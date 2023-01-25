package net.luversof.bbs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bbs.domain.Bbs;
import net.luversof.bbs.service.BbsService;

@Slf4j
class BbsTest implements GeneralTest {

	@Autowired
	private BbsService bbsService;
	
	@Test
	void test() {
		Bbs bbs = new Bbs();
		bbs.setAlias("free");
		bbsService.save(bbs);
		log.debug("bbs : {}", bbs);
	}
	
	@Test
	void test2() {
		Bbs bbs = bbsService.findByAlias("free");
		log.debug("bbs : {}", bbs);
		Bbs bbs2 = bbsService.findByAlias("free2");
		log.debug("bbs : {}", bbs2);
	}
}
