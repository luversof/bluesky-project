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
import net.luversof.api.stock.service.StockPriceService;
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

		var tradeCsvRecordList = loadTradeCsvRecordList();

		// 계좌 이름별로 계좌 찾기 또는 생성
		var accountMap = new java.util.HashMap<String, UUID>();
		var existingAccounts = accountRepository.findByUserId(userId);

		// 기존 계좌 맵에 추가
		existingAccounts.forEach(account -> accountMap.put(account.getName(), account.getId()));

		// CSV에서 새로운 계좌 이름 찾기
		tradeCsvRecordList.stream()
				.map(TradeCsvRecord::get계좌)
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

		// tradeCsvRecordList를 trade로 변환
		var tradeList = tradeCsvRecordList.stream().map(t -> {
			var trade = t.toTrade();

			// 계좌 이름으로 accountId 설정
			String accountName = t.get계좌();
			if (accountName != null && !accountName.isBlank()) {
				UUID accountId = accountMap.get(accountName);
				if (accountId != null) {
					trade.setAccountId(accountId);
				} else {
					log.warn("Account not found for name: {}", accountName);
				}
			}

			var stockItem = stockItemList.stream()
					.filter(s -> s.getName().equals(t.get종목()))
					.findFirst()
					.orElseGet(() -> null);

			if (stockItem == null) {
				log.debug("stockItem not found : {}", t.get종목());
				return null; // stockItem이 없으면 trade를 생성하지 않음
			}

			trade.setStockItemId(stockItem.getId());

			// 현재가 정보가 있으면 StockPrice에 저장
			if (t.get현재가() != null) {
				stockPriceService.savePrice(stockItem.getId(), t.get현재가());
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
				.readValues(new ClassPathResource("data/trade.csv").getInputStream());

		var stockItemList = it.readAll();
		log.debug("items : {}", stockItemList.size());
		return stockItemList;
	}

}