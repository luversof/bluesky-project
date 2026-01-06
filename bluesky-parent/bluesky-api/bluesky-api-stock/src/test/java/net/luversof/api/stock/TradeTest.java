package net.luversof.api.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.databind.TradeTypeDeserializer;
import net.luversof.api.stock.domain.GoogleSheetsTrade;
import net.luversof.api.stock.repository.AccountRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.service.GoogleSheetsTestService;
import net.luversof.api.stock.service.StockPriceService;
import net.luversof.api.stock.service.TradeService;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

class TradeTest implements GeneralTest {

	private static final Logger log = LoggerFactory.getLogger(TradeTest.class);
	
	@Autowired
	GoogleSheetsTestService googleSheetsTestService;

	@Autowired
	TradeService tradeService;

	@Autowired
	TradeRepository tradeRepository;

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	StockItemRepository stockItemRepository;

	@Autowired
	StockPriceService stockPriceService;

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
		tradeRepository.deleteAll();

		var stockItemList = StreamSupport.stream(stockItemRepository.findAll().spliterator(), false).toList();

		var googleSheetsTradeList = loadGoogleSheetsTradeList();

		// 계좌 이름별로 계좌 찾기 또는 생성
		var accountMap = new HashMap<String, UUID>();
		var existingAccounts = accountRepository.findByUserId(userId);

		// 기존 계좌 맵에 추가
		existingAccounts.forEach(account -> accountMap.put(account.getName(), account.getId()));

		// CSV에서 새로운 계좌 이름 찾기
		googleSheetsTradeList.stream()
				.map(GoogleSheetsTrade::get계좌)
				.filter(accountName -> accountName != null && !accountName.isBlank())
				.distinct()
				.filter(accountName -> !accountMap.containsKey(accountName))
				.forEach(accountName -> {
					// 새 계좌 생성
					var newAccount = new net.luversof.api.stock.domain.Account();
					newAccount.setUserId(userId);
					newAccount.setName(accountName);
					var savedAccount = accountRepository.save(newAccount);
					accountMap.put(accountName, savedAccount.getId());
					log.debug("Created new account: {} with id: {}", accountName, savedAccount.getId());
				});

		// googleSheetsTradeList를 tradeList로 변환
		var tradeList = googleSheetsTradeList
				.stream()
				.map(t -> {
					var trade = t.toTrade(accountMap, stockItemList);
		
					// 현재가 정보가 있으면 StockPrice에 저장
					if (trade != null && t.get현재가() != null) {
						stockPriceService.savePrice(trade.getStockItemId(), t.get현재가());
					}
		
					return trade;
				})
				.filter(trade -> trade != null) // null 제거
				.toList();
		log.debug("tradeList : {}", tradeList);

		var result = tradeRepository.saveAll(tradeList);
		log.debug("result : {}", result);
	}

	@Test
	void loadTest() throws IOException {
		var tradeCsvRecordList = loadTradeCsvRecordList();
		assertThat(tradeCsvRecordList.size() > 0);
	}
	
	List<GoogleSheetsTrade> loadGoogleSheetsTradeList() {
		return googleSheetsTestService.getList(GoogleSheetsApiCase.GoogleSheetsTrade);
	}

	List<GoogleSheetsTrade> loadTradeCsvRecordList() throws IOException {

		var mapper = new CsvMapper();

		SimpleModule module = new SimpleModule();
		module.addDeserializer(TradeType.class, new TradeTypeDeserializer());
		mapper.registeredModules().add(module);

		MappingIterator<GoogleSheetsTrade> it = mapper
				.readerFor(GoogleSheetsTrade.class)
				.with(CsvSchema.emptySchema().withHeader())
				.readValues(new ClassPathResource("data/trade.csv").getInputStream());

		var stockItemList = it.readAll();
		log.debug("items : {}", stockItemList.size());
		return stockItemList;
	}

}