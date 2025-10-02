package net.luversof.api.stock;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.service.TradeService;

@Slf4j
class TradeTest implements GeneralTest {
	
	@Autowired
	TradeService tradeService;
	
	@Autowired
	StockItemRepository stockItemRepository;
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	UUID userId = TestConstant.USER_ID;

	@Test
	void test() {
		var tradeList = tradeService.findByAccountId(userId);
		log.debug("tradeList : {}", tradeList);
	}
	
	// excel csv로 대량 insert 예제
	@Test
	void tradeBulkInsert() {
		String sql = """
				INSERT INTO "Trade" (id, account_id, stockItem_id, type, quantity, price, fee, tax, tradeDate)
				""";
		
		
		// 저장 하기 전에 stockItem_id를 셋팅 해야 함.
		var stockItemList = stockItemRepository.findAll();
		
		
		
	}
	
	@SneakyThrows
	List<Trade> loadCsvStockItemList() {
		var mapper = new CsvMapper();
		MappingIterator<Trade> it = mapper
				.readerFor(Trade.class)
				.with(CsvSchema.emptySchema().withHeader())
				.readValues(new ClassPathResource("data/trade.csv").getInputStream())
				;
		
		var stockItemList = it.readAll();
		log.debug("items : {}", stockItemList.size());
		return stockItemList;
	}
}
