package net.luversof.bookkeeping;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.BookkeepingRepository;
import net.luversof.bookkeeping.service.BookkeepingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Slf4j
public class BookkeepingTest extends GeneralTest {

    @Autowired
    private BookkeepingService bookkeepingService;

    @Autowired
    private BookkeepingRepository bookkeepingRepository;

    static final UUID TEST_USER_ID = UUID.fromString("35929103-da22-49e7-9d76-214bb081593f");

    @Test
    public void save() {
        Bookkeeping bookkeeping = new Bookkeeping();
        bookkeeping.setName("test2");
        bookkeeping.setUserId(TEST_USER_ID);
        log.debug("bookkeeping : {}", bookkeeping);
        Bookkeeping result = bookkeepingService.create(bookkeeping);
        log.debug("bookkeeping : {}", bookkeeping);
        log.debug("result : {}", result);
    }

    @Test
    public void update() {
        Bookkeeping bookkeeping = new Bookkeeping();
        bookkeeping.setUserId(TEST_USER_ID);
        bookkeeping.setBaseDate(11);
        Bookkeeping updateBookkeeping = bookkeepingService.update(bookkeeping);
        log.debug("bookkeeping : {}", updateBookkeeping);
    }

    @Test
    public void findByUserId() {
        log.debug("bookkeeping : {}", bookkeepingService.findByUserId(TEST_USER_ID));
    }

    @Test
    public void delete() {
        Bookkeeping bookkeeping = new Bookkeeping();
        bookkeeping.setId(UUID.fromString("35929103-da22-49e7-9d76-214bb081593f"));
        bookkeeping.setUserId(UUID.fromString("2e85288b-5d0a-4626-9393-f2f9bbd8ba4a"));
        bookkeepingService.delete(bookkeeping);
    }
}
