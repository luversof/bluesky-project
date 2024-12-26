package net.luversof.api.bookkeeping.composite;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.constant.TestConstant;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.service.CompositeService;

@Slf4j
class BookkeepingCompositeServiceTest implements GeneralTest {
	
	@Autowired
	private CompositeService bookkeepingCompositeService;
	
	@Test
	void create() {
		var bookeeping = new Bookkeeping();
		bookeeping.setUserId(TestConstant.USER_ID);
		
		bookkeepingCompositeService.initDataSetup(bookeeping);
		
		log.debug("bookkeeping : {}", bookeeping);
	}

}
