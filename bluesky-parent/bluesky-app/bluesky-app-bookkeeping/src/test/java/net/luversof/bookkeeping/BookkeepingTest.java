package net.luversof.bookkeeping;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.BookkeepingService;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class BookkeepingTest extends GeneralTest {

	@Autowired
	private BookkeepingService bookkeepingService;
	
	static final String TEST_USER_ID = "1";
	
	@Test
	public void save() {
		Bookkeeping bookkeeping = new Bookkeeping();
		bookkeeping.setUserId(TEST_USER_ID);
		bookkeeping.setName("test");
		Bookkeeping result = bookkeepingService.create(bookkeeping);
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
	
	@Test
	public void delete() {
		Bookkeeping bookkeeping = new Bookkeeping();
		bookkeeping.setId(5);
		bookkeeping.setUserId("2");
		bookkeepingService.delete(bookkeeping);
	}
}
