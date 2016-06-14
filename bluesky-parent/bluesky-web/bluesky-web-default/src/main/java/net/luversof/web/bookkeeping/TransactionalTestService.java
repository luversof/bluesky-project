package net.luversof.web.bookkeeping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.core.exception.BlueskyException;

/**
 * multi transactional 테스트
 * @author bluesky
 *
 */
@Slf4j
@Service
//@Transactional(value = "bookkeepingTransactionManager")
public class TransactionalTestService {

	@Autowired
	private BookkeepingService bookkeepingService;
	
	
	public void test() {
		Bookkeeping bookkeeping = new Bookkeeping();
		bookkeeping.setUserId(2);
		bookkeeping.setName("test");
		Bookkeeping result = bookkeepingService.create(bookkeeping);
		

		log.debug("result : {}", result);
		//throw new BlueskyException("test");
	}
	
}
