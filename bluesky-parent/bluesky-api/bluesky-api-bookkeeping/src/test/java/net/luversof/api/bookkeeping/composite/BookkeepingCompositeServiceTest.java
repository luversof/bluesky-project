package net.luversof.api.bookkeeping.composite;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.base.domain.Bookkeeping;
import net.luversof.api.bookkeeping.composite.service.BookkeepingCompositeService;
import net.luversof.api.bookkeeping.constant.TestConstant;

@Slf4j
class BookkeepingCompositeServiceTest implements GeneralTest {
	
	@Autowired
	private BookkeepingCompositeService bookkeepingCompositeService;
	
	@Test
	void create() {
		var bookeeping = new Bookkeeping();
		bookeeping.setUserId(TestConstant.USER_ID);
		
		bookkeepingCompositeService.create(bookeeping);
		
		log.debug("bookkeeping : {}", bookeeping);
	}

}
