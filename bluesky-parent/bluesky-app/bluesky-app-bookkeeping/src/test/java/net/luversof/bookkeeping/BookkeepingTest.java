package net.luversof.bookkeeping;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.BookkeepingService;

import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class BookkeepingTest extends GeneralTest {

	@Autowired
	private BookkeepingService bookkeepingService;
	
	static final UUID TEST_USER_ID = UUID.randomUUID();
	
	@Test
	public void save() {
		Bookkeeping bookkeeping = new Bookkeeping();
		bookkeeping.setName("test2");
		log.debug("bookkeeping : {}", bookkeeping);
		Bookkeeping result = bookkeepingService.create(bookkeeping);
		log.debug("bookkeeping : {}", bookkeeping);
		log.debug("result : {}", result);
	}
	
	@Test
	public void findOne() {
		log.debug("bookkeeping : {}", bookkeepingService.findById(TEST_USER_ID));
	}
	
	@Test
	public void findByUserId() {
		log.debug("bookkeeping : {}", bookkeepingService.findByUserId(TEST_USER_ID));
	}
	
	@Test
	public void delete() {
		Bookkeeping bookkeeping = new Bookkeeping();
		bookkeeping.setId(UUID.fromString("5"));
		bookkeeping.setUserId(UUID.fromString("2"));
		bookkeepingService.delete(bookkeeping);
	}
}
