package net.luversof.web.gate.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.luversof.GeneralWebTest;
import net.luversof.web.gate.bookkeeping.domain.Bookkeeping;
import net.luversof.web.gate.bookkeeping.httpexchange.BookkeepingClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BookkeepingClientTest implements GeneralWebTest {

    @Autowired private BookkeepingClient bookkeepingClient;

    @Test
    void beanCheck() {
        assertThat(bookkeepingClient).isNotNull();
    }

    @Test
    void findByUserId() {
        List<Bookkeeping> bookkeepingList = bookkeepingClient.findByUserId("string");
        assertThat(bookkeepingList).isNotEmpty();
    }
}
