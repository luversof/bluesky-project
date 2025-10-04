package net.luversof.api.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.databind.TradeTypeDeserializer;
import net.luversof.api.stock.domain.TradeCsvRecord;
import net.luversof.api.stock.repository.AccountRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.service.TradeService;

@Slf4j
class TradeTest implements GeneralTest {
	
	@Autowired
	TradeService tradeService;
	
	@Autowired
	TradeRepository tradeRepository;
	
	@Autowired
	AccountRepository accountRepository;
	
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
		var account = accountRepository.findByUserId(userId).get(0);
		
		;
		var stockItemList = StreamSupport.stream(stockItemRepository.findAll().spliterator(), false).toList();
		
		var tradeCsvRecordList = loadTradeCsvRecordList();
		// tradeCsvRecordList를 trade로 변환
		var tradeList = tradeCsvRecordList.stream().map(t -> {
			var trade = t.toTrade();
			trade.setAccountId(account.getId());
			var stockItem = stockItemList.stream()
					.filter(s -> s.getName().equals(t.get종목()))
					.findFirst()
					.orElseGet(() -> null);
			
			if (stockItem == null) {
				log.debug("stockItem not found : {}", t.get종목());
			}
			
			trade.setStockItemId(stockItem.getId());
			return trade;
		}).toList();
		log.debug("tradeList : {}", tradeList);
		
		var result = tradeRepository.saveAll(tradeList);
		log.debug("result : {}", result);
	}
	
	@Test
	void loadTest() {
		var tradeCsvRecordList = loadTradeCsvRecordList();
		assertThat(tradeCsvRecordList.size() > 0);
	}
	
	@SneakyThrows
	List<TradeCsvRecord> loadTradeCsvRecordList() {
		
		
		var mapper = new CsvMapper();
		
		SimpleModule module = new SimpleModule();
		module.addDeserializer(TradeType.class, new TradeTypeDeserializer());
		mapper.registerModule(module);
		
		MappingIterator<TradeCsvRecord> it = mapper
				.readerFor(TradeCsvRecord.class)
				.with(CsvSchema.emptySchema().withHeader())
				.readValues(new ClassPathResource("data/trade.csv").getInputStream())
				;
		
		var stockItemList = it.readAll();
		log.debug("items : {}", stockItemList.size());
		return stockItemList;
	}
	
}