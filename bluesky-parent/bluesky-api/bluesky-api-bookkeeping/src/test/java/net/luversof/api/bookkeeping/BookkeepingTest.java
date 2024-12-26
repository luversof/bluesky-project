package net.luversof.api.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.constant.TestConstant;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.repository.mariadb.BookkeepingRepository;

@Slf4j
class BookkeepingTest implements GeneralTest {
	
	@Autowired
	private BookkeepingRepository bookkeepingRepository;

	@Test
	@DisplayName("저장 테스트")
	void createBookeeping() {
		var userId = TestConstant.USER_ID;
		// 대상이 없으면 새로 만들어서 저장
		List<Bookkeeping> bookkeepingList = bookkeepingRepository.findByUserId(userId);
		if (!bookkeepingList.isEmpty()) {
			log.debug("already create bookkeeping for {}", userId);
			return;
		}
		
		var bookkeeping = new Bookkeeping();
		bookkeeping.setUserId(userId);
		bookkeeping.setName("bookkeeping of " + userId);
		
		Bookkeeping save = bookkeepingRepository.save(bookkeeping);
		assertThat(save).isNotNull();
	}
	
	
	@Test
	void test() {
		List<Bookkeeping> bookkeepingList = bookkeepingRepository.findByUserId(TestConstant.USER_ID);
		log.debug("bookkeepingList : {}", bookkeepingList);
	}
	
}
