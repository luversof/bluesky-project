package net.luversof.bookkeeping;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.CompositeBookkeepingService;

@Slf4j
public class BookkeepingTest extends GeneralTest {

    @Autowired
    private CompositeBookkeepingService bookkeepingService;

    @Test
    public void save() {
        Bookkeeping bookkeeping = new Bookkeeping();
        bookkeeping.setName("test2");
        bookkeeping.setUserId(BookkeepingTestConstant.USER_ID);
        log.debug("bookkeeping : {}", bookkeeping);
        Bookkeeping result = bookkeepingService.create(bookkeeping);
        log.debug("bookkeeping : {}", bookkeeping);
        log.debug("result : {}", result);
    }
    
    @Test
    public void findByUserId() {
    	log.debug("bookkeeping : {}", bookkeepingService.findByUserId(BookkeepingTestConstant.USER_ID));
    }

    @Test
    public void update() {
    	var bookkeeping = bookkeepingService.findByUserId(BookkeepingTestConstant.USER_ID).stream().findFirst().get();
        bookkeeping.setUserId(BookkeepingTestConstant.USER_ID);
        bookkeeping.setBaseDate(11);
        Bookkeeping updateBookkeeping = bookkeepingService.update(bookkeeping);
        log.debug("bookkeeping : {}", updateBookkeeping);
    }

    @Test
    public void delete() {
    	var bookkeeping = bookkeepingService.findByUserId(BookkeepingTestConstant.USER_ID).stream().findFirst().get();
        bookkeepingService.delete(bookkeeping);
    }
}
