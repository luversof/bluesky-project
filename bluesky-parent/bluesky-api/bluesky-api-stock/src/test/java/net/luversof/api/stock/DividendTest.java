package net.luversof.api.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.Dividend;
//import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.GoogleSheetsDividend;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.AccountRepository;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.service.DividendService;
import net.luversof.api.stock.service.GoogleSheetsTestService;
import net.luversof.api.stock.web.dto.request.DividendSearchRequest;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

class DividendTest implements GeneralTest {
	
	private static final Logger log = LoggerFactory.getLogger(DividendTest.class);

	@Autowired
	GoogleSheetsTestService googleSheetsTestService;

	@Autowired
	DividendRepository dividendRepository;

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	StockItemRepository stockItemRepository;

	@Autowired
	DividendService dividendService;

	UUID userId = TestConstant.USER_ID;

	@Test
	void dividendBulkInsert() throws IOException {
		dividendRepository.deleteAll();

		var googleSheetsDividendList = loadGoogleSheetsDividendList();
		assertThat(googleSheetsDividendList).isNotEmpty();

		var accountMap = prepareAccountMap(googleSheetsDividendList);
		var stockItemMap = prepareStockItemMap(googleSheetsDividendList);

		var dividends = googleSheetsDividendList.stream()
				.map(googleSheetsDividend -> googleSheetsDividend.toDividend( accountMap, stockItemMap))
				.filter(java.util.Objects::nonNull)
				.toList();

		assertThat(dividends).isNotEmpty();

		var savedDividends = StreamSupport.stream(dividendRepository.saveAll(dividends).spliterator(), false).toList();
		assertThat(savedDividends).hasSize(dividends.size());

		// Ensure service.findDividends returns stockItemId populated
		DividendSearchRequest request = new DividendSearchRequest();
		request.setUserId(userId);
		List<Dividend> found = dividendService.findDividends(request);
		assertThat(found).isNotEmpty();
		found.forEach(d -> assertThat(d.getStockItemId()).isNotNull());
	}

	private Map<String, UUID> prepareAccountMap(List<GoogleSheetsDividend> records) {
		var accountMap = accountRepository.findByUserId(userId).stream()
				.collect(Collectors.toMap(Account::getName, Account::getId, (left, _) -> left,
						java.util.LinkedHashMap::new));

		records.stream()
				.map(GoogleSheetsDividend::get계좌)
				.filter(StringUtils::hasText)
				.map(String::trim)
				.forEach(accountName -> accountMap.computeIfAbsent(accountName, name -> {
					var newAccount = new Account();
					newAccount.setUserId(userId);
					newAccount.setName(name);
					var savedAccount = accountRepository.save(newAccount);
					log.debug("Created account for dividend import: {}", name);
					return savedAccount.getId();
				}));

		return accountMap;
	}

	private Map<String, UUID> prepareStockItemMap(List<GoogleSheetsDividend> records) {
		var stockItemMap = StreamSupport.stream(stockItemRepository.findAll().spliterator(), false)
				.collect(Collectors.toMap(StockItem::getName, StockItem::getId, (left, _) -> left,
						java.util.LinkedHashMap::new));

		records.stream()
				.map(GoogleSheetsDividend::get종목)
				.filter(StringUtils::hasText)
				.map(String::trim)
				.forEach(stockName -> stockItemMap.computeIfAbsent(stockName, name -> {
					var newStockItem = new StockItem();
					newStockItem.setName(name);
					newStockItem.setMarket("KOSPI");
					newStockItem.setSymbol(generateSymbol(name));
					var savedStockItem = stockItemRepository.save(newStockItem);
					log.debug("Created stock item for dividend import: {}", name);
					return savedStockItem.getId();
				}));

		return stockItemMap;
	}

	private String generateSymbol(String baseName) {
		var alphanumeric = baseName == null ? "" : baseName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
		if (!StringUtils.hasText(alphanumeric)) {
			alphanumeric = "DIV";
		}
		var randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
		var candidate = (alphanumeric + randomSuffix);
		return candidate.substring(0, Math.min(candidate.length(), 12));
	}

	
	List<GoogleSheetsDividend> loadGoogleSheetsDividendList() {
		return googleSheetsTestService.getList(GoogleSheetsApiCase.GoogleSheetsDividend);
	}

	List<GoogleSheetsDividend> loadDividendCsvRecordList() throws IOException {
		var mapper = new CsvMapper();
		MappingIterator<GoogleSheetsDividend> iterator = mapper
				.readerFor(GoogleSheetsDividend.class)
				.with(CsvSchema.emptySchema().withHeader())
				.readValues(new ClassPathResource("data/divedend.csv").getInputStream());
		var records = iterator.readAll();
		log.debug("Loaded {} dividend rows", records.size());
		return records;
	}

	@Test
	void selectAllDividends() {
		var all = StreamSupport.stream(dividendRepository.findAll().spliterator(), false).toList();
		log.info("Total dividends in DB: {}", all.size());
		all.forEach(d -> log.info("Dividend id={}, accountId={}, stockItemId={}, stockItemName={}",
				d.getId(), d.getAccountId(), d.getStockItemId(), d.getStockItemName()));
		assertThat(all).isNotNull();
	}

}
