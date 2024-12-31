package net.luversof.api.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.constant.TestConstant;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.service.BookkeepingService;

@Slf4j
class BookkeepingTest implements GeneralTest {
	
	@Setter(onMethod_ = @Autowired)
	private BookkeepingService bookkeepingService;
	
	private UUID userId = TestConstant.USER_ID;
	private UUID bookkeepingId = TestConstant.BOOKKEEPING_ID;
	
	@Test
	@DisplayName("초기 데이터 생성")
	void createBookkeeping() {
		
		var bookkeeping = new Bookkeeping();
		bookkeeping.setUserId(userId);
		bookkeeping.setId(bookkeepingId);
		var bookkeepingResult = bookkeepingService.createBookkeeping(bookkeeping);
		log.debug("bookkeepingResult : {}", bookkeepingResult);
		assertThat(bookkeepingResult).isNotNull();
	}

	@Test
	@DisplayName("해당 유저의 가계부 데이터 일괄 삭제")
	void deleteBookkeepingByUserId() {
		bookkeepingService.deleteBookkeepingByUserId(userId);
	}
}
