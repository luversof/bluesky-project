package net.luversof.api.bookkeeping.composite;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.constant.TestConstant;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.service.BookkeepingService;

class BookkeepingCompositeServiceTest implements GeneralTest {

	private static final Logger log = LoggerFactory.getLogger(BookkeepingCompositeServiceTest.class);

	@Autowired
	private BookkeepingService bookkeepingCompositeService;

	@Test
	void create() {
		var bookeeping = new Bookkeeping();
		bookeeping.setUserId(TestConstant.USER_ID);

		bookkeepingCompositeService.createBookkeeping(bookeeping);

		log.debug("bookkeeping : {}", bookeeping);
	}

}
