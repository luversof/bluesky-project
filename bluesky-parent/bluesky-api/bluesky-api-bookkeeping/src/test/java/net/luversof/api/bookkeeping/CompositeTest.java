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
import net.luversof.api.bookkeeping.service.CompositeService;

@Slf4j
class CompositeTest implements GeneralTest {
	
	@Setter(onMethod_ = @Autowired)
	private CompositeService compositeService;
	
	private UUID userId = TestConstant.USER_ID;
	
	@Test
	@DisplayName("초기 데이터 생성")
	void initDataSetup() {
		
		var bookkeeping = new Bookkeeping();
		bookkeeping.setUserId(userId);
		var bookkeepingResult = compositeService.initDataSetup(bookkeeping);
		log.debug("bookkeepingResult : {}", bookkeepingResult);
		assertThat(bookkeepingResult).isNotNull();
	}

	@Test
	@DisplayName("해당 유저의 전체 데이터 삭제")
	void deleteAllDataByUserId() {
		compositeService.deleteAllDataByUserId(userId);
	}
}
