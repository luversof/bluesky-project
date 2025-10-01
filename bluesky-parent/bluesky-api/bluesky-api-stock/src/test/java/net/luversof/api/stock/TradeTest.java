package net.luversof.api.stock;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.service.TradeService;

@Slf4j
public class TradeTest implements GeneralTest {
	
	@Autowired
	TradeService tradeService;
	
	UUID userId = TestConstant.USER_ID;

	@Test
	void test() {
		var tradeList = tradeService.findByAccountId(userId);
		log.debug("tradeList : {}", tradeList);
	}
	
	// excel csv로 대량 insert 예제
}
