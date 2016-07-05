package net.luversof.bbs;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bbs.domain.Bbs;
import net.luversof.bbs.service.BbsService;

@Slf4j
public class BbsTest extends GeneralTest {

	@Autowired
	private BbsService bbsService;
	
	@Test
	public void test() {
		Bbs bbs = new Bbs();
		bbs.setAliasName("free");
		bbsService.save(bbs);
		log.debug("bbs : {}", bbs);
	}
}
