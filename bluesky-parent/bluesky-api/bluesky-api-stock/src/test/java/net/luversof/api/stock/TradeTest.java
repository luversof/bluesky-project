package net.luversof.api.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.service.AccountTestService;
import net.luversof.api.stock.service.TradeService;
import net.luversof.app.google.stock.domain.GoogleSheetTrade;
import net.luversof.app.google.stock.service.StockGoogleSheetService;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

class TradeTest implements GeneralTest {

	private static final Logger log = LoggerFactory.getLogger(TradeTest.class);

	@Autowired
	StockGoogleSheetService stockGoogleSheetService;
	
	@Autowired
	TradeService tradeService;

	@Autowired
	TradeRepository tradeRepository;

	@Autowired
	AccountTestService accountTestService;

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
		tradeRepository.deleteAll();

		var stockItemList = StreamSupport.stream(stockItemRepository.findAll().spliterator(), false).toList();

		var googleSheetsTradeList = stockGoogleSheetService.getGoogleSheetTradeList(TestConstant.USER_ID);

		// 계좌 이름별로 계좌 찾기 또는 생성
		var accountMap = new HashMap<String, UUID>();
		var existingAccounts = accountTestService.findByUserId(userId);

		// 기존 계좌 맵에 추가
		existingAccounts.forEach(account -> accountMap.put(account.getName(), account.getId()));

		// CSV에서 새로운 계좌 이름 찾기
		googleSheetsTradeList.stream()
				.map(GoogleSheetTrade::get계좌)
				.filter(accountName -> accountName != null && !accountName.isBlank())
				.distinct()
				.filter(accountName -> !accountMap.containsKey(accountName))
				.forEach(accountName -> {
					// 새 계좌 생성
					var newAccount = new net.luversof.api.stock.domain.Account();
					newAccount.setUserId(userId);
					newAccount.setName(accountName);
					var savedAccount = accountTestService.save(newAccount);
					accountMap.put(accountName, savedAccount.getId());
					log.debug("Created new account: {} with id: {}", accountName, savedAccount.getId());
				});

		// googleSheetsTradeList를 tradeList로 변환
		var tradeList = googleSheetsTradeList
				.stream()
				.map(t -> toTrade(t, accountMap, stockItemList))
				.filter(Objects::nonNull) // null 제거
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
	
	List<GoogleSheetTrade> loadGoogleSheetsTradeList() {
		return stockGoogleSheetService.getGoogleSheetTradeList(TestConstant.USER_ID);
	}

	List<GoogleSheetTrade> loadTradeCsvRecordList() throws IOException {

		SimpleModule module = new SimpleModule();
		module.addDeserializer(TradeType.class, new TradeTypeDeserializer());
		
		var mapper = CsvMapper.builder()
				.addModule(module)
				.build();

		MappingIterator<GoogleSheetTrade> it = mapper
				.readerFor(GoogleSheetTrade.class)
				.with(CsvSchema.emptySchema().withHeader())
				.readValues(new ClassPathResource("data/trade.csv").getInputStream());

		var stockItemList = it.readAll();
		log.debug("items : {}", stockItemList.size());
		return stockItemList;
	}
	
	
	public Trade toTrade(GoogleSheetTrade googleSheetTrade, HashMap<String, UUID> accountMap, List<StockItem> stockItemList) {
		Trade trade = new Trade();
		trade.setType(googleSheetTrade.get구분().equals("매수") ? TradeType.BUY : TradeType.SELL);
		trade.setQuantity(googleSheetTrade.get매매_수량());
		trade.setPrice(googleSheetTrade.get매매가());
		trade.setFee(googleSheetTrade.get수수료() == null ? BigDecimal.ZERO : googleSheetTrade.get수수료());
		trade.setTax(googleSheetTrade.get거래세() == null ? BigDecimal.ZERO : googleSheetTrade.get거래세());

		trade.setTradeDate(googleSheetTrade.get날짜());

		// 계좌 이름으로 accountId 설정
		String accountName = googleSheetTrade.get계좌();
		if (accountName != null && !accountName.isBlank()) {
			UUID accountId = accountMap.get(accountName);
			if (accountId != null) {
				trade.setAccountId(accountId);
			} else {
				log.warn("Account not found for name: {}", accountName);
			}
		}

		var stockItem = stockItemList.stream()
				.filter(s -> s.getName().equals(googleSheetTrade.get종목()))
				.findFirst()
				.orElseGet(() -> null);

		if (stockItem == null) {
			log.debug("stockItem not found : {}", googleSheetTrade.get종목());
			return null; // stockItem이 없으면 trade를 생성하지 않음
		}

		trade.setStockItemId(stockItem.getId());

		return trade;
	}

}