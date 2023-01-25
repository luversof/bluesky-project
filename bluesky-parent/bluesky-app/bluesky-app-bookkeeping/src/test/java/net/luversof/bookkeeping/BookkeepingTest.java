package net.luversof.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.CompositeBookkeepingService;

@Slf4j
class BookkeepingTest implements GeneralTest {

    @Autowired
    private CompositeBookkeepingService bookkeepingService;

    @Test
    @DisplayName("Bookkeeping 생성")
    void create() {
        Bookkeeping bookkeeping = new Bookkeeping();
        bookkeeping.setName("test2");
        bookkeeping.setUserId(BookkeepingTestConstant.USER_ID);
        log.debug("bookkeeping : {}", bookkeeping);
        Bookkeeping result = bookkeepingService.create(bookkeeping);
        assertThat(result).isNotNull();
        log.debug("bookkeeping : {}", bookkeeping);
        log.debug("result : {}", result);
    }
    
    @Test
    void findByUserId() {
    	var bookkeepingList = bookkeepingService.findByUserId(BookkeepingTestConstant.USER_ID);
    	assertThat(bookkeepingList).isNotEmpty();
    	log.debug("bookkeepingList : {}", bookkeepingList);
    }

    @Test
    void update() {
    	var bookkeeping = bookkeepingService.findByUserId(BookkeepingTestConstant.USER_ID).stream().findFirst().get();
        bookkeeping.setUserId(BookkeepingTestConstant.USER_ID);
        bookkeeping.setBaseDate(11);
        Bookkeeping updateBookkeeping = bookkeepingService.update(bookkeeping);
        log.debug("bookkeeping : {}", updateBookkeeping);
    }

    @Test
    void delete() {
    	var bookkeeping = bookkeepingService.findByUserId(BookkeepingTestConstant.USER_ID).stream().findFirst().get();
        bookkeepingService.delete(bookkeeping);
    }
}
