package net.luversof.api.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luversof.boot.jdbc.datasource.context.RoutingDataSourceContextHolder;
import java.util.UUID;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.constant.TestConstant;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.service.BookkeepingService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

class BookkeepingTest implements GeneralTest {

    private static final Logger log = LoggerFactory.getLogger(BookkeepingTest.class);

    @Autowired BookkeepingService bookkeepingService;

    UUID userId = TestConstant.USER_ID;
    UUID bookkeepingId = TestConstant.BOOKKEEPING_ID;

    @BeforeAll
    static void beforeAll() {
        RoutingDataSourceContextHolder.setContext(() -> "bookkeeping_postgresql");
    }

    @Test
    @DisplayName("초기 데이터 생성")
    void createBookkeeping() {

        var bookkeeping = new Bookkeeping();
        bookkeeping.setUserId(userId);
        bookkeeping.setName("테스트 가계부");
        var bookkeepingResult = bookkeepingService.createBookkeeping(bookkeeping);
        log.debug("bookkeepingResult : {}", bookkeepingResult);
        assertThat(bookkeepingResult).isNotNull();

        // assetId를 테스트 id로 변경하고 싶은데...

    }

    @Test
    @DisplayName("해당 유저의 가계부 데이터 일괄 삭제")
    void deleteBookkeepingByUserId() {
        bookkeepingService.deleteAllByUserId(userId);
        log.debug("삭제 완료 : {}", userId);
    }
}
