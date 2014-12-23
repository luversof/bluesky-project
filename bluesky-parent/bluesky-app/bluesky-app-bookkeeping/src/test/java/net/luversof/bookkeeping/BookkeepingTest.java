package net.luversof.bookkeeping;

import lombok.extern.slf4j.Slf4j;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.BookkeepingService;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class BookkeepingTest extends GeneralTest {

	@Autowired
	private BookkeepingService bookkeepingService;
	
	@Test
	public void save() {
		Bookkeeping bookkeeping = new Bookkeeping();
		bookkeeping.setUserId(TEST_USER_ID);
		bookkeeping.setName("test");
		Bookkeeping result = bookkeepingService.save(bookkeeping);
		log.debug("bookkeeping : {}", bookkeeping);
		log.debug("result : {}", result);
	}
	
	@Test
	public void findOne() {
		log.debug("bookkeeping : {}", bookkeepingService.findOne(1));
	}
	
	@Test
	public void findByUserId() {
		log.debug("bookkeeping : {}", bookkeepingService.findByUserId(TEST_USER_ID));
	}
}
