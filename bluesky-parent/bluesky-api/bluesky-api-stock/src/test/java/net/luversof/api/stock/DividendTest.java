package net.luversof.api.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.service.AccountTestService;
import net.luversof.api.stock.service.DividendService;
import net.luversof.api.stock.web.dto.request.DividendSearchRequest;
import net.luversof.app.google.stock.domain.GoogleSheetDividend;
import net.luversof.app.google.stock.service.StockGoogleSheetService;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

class DividendTest implements GeneralTest {
	
	private static final Logger log = LoggerFactory.getLogger(DividendTest.class);

	@Autowired
	StockGoogleSheetService stockGoogleSheetService;

	@Autowired
	DividendRepository dividendRepository;

	@Autowired
	AccountTestService accountTestService;

	@Autowired
	StockItemRepository stockItemRepository;

	@Autowired
	DividendService dividendService;

	UUID userId = TestConstant.USER_ID;

	@Test
	void dividendBulkInsert() throws IOException {
		dividendRepository.deleteAll();

		var googleSheetsDividendList = stockGoogleSheetService.getGoogleSheetDividendList(TestConstant.USER_ID);
		assertThat(googleSheetsDividendList).isNotEmpty();

		var accountMap = prepareAccountMap(googleSheetsDividendList);
		var stockItemMap = prepareStockItemMap(googleSheetsDividendList);

		var dividends = googleSheetsDividendList.stream()
				.map(googleSheetsDividend -> toDividend(googleSheetsDividend, accountMap, stockItemMap))
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

	private Map<String, UUID> prepareAccountMap(List<GoogleSheetDividend> records) {
		var accountMap = accountTestService.findByUserId(userId).stream()
				.collect(Collectors.toMap(Account::getName, Account::getId, (left, _) -> left,
						java.util.LinkedHashMap::new));

		records.stream()
				.map(GoogleSheetDividend::get계좌)
				.filter(StringUtils::hasText)
				.map(String::trim)
				.forEach(accountName -> accountMap.computeIfAbsent(accountName, name -> {
					var newAccount = new Account();
					newAccount.setUserId(userId);
					newAccount.setName(name);
					var savedAccount = accountTestService.save(newAccount);
					log.debug("Created account for dividend import: {}", name);
					return savedAccount.getId();
				}));

		return accountMap;
	}

	private Map<String, UUID> prepareStockItemMap(List<GoogleSheetDividend> records) {
		var stockItemMap = StreamSupport.stream(stockItemRepository.findAll().spliterator(), false)
				.collect(Collectors.toMap(StockItem::getName, StockItem::getId, (left, _) -> left,
						java.util.LinkedHashMap::new));

		records.stream()
				.map(GoogleSheetDividend::get종목)
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


	List<GoogleSheetDividend> loadDividendCsvRecordList() throws IOException {
		var mapper = new CsvMapper();
		MappingIterator<GoogleSheetDividend> iterator = mapper
				.readerFor(GoogleSheetDividend.class)
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
	
	public Dividend toDividend(GoogleSheetDividend googleSheetsDividend, Map<String, UUID> accountMap, Map<String, UUID> stockItemMap) {
		var accountName = googleSheetsDividend.get계좌();
		var stockName = googleSheetsDividend.get종목();

		if (!StringUtils.hasText(accountName) || !StringUtils.hasText(stockName)) {
			return null;
		}

		var accountId = accountMap.get(accountName.trim());
		var stockItemId = stockItemMap.get(stockName.trim());
		var payDate = parsePayDate(googleSheetsDividend.get지급일());

		if (accountId == null || stockItemId == null || payDate == null) {
			log.warn("Skip dividend row. accountId: {}, stockItemId: {}, payDate: {}", accountId, stockItemId, payDate);
			return null;
		}

		var dividend = new Dividend();
		dividend.setAccountId(accountId);
		dividend.setStockItemId(stockItemId);
		dividend.setType("DIVIDEND");
		dividend.setQuantity(googleSheetsDividend.get주식수());
		dividend.setAmountPerShare(googleSheetsDividend.get배당금());
		dividend.setTaxPerShare(googleSheetsDividend.get주당과세표준액());
		dividend.setGrossAmount(googleSheetsDividend.get배당금() == null ? BigDecimal.ZERO : googleSheetsDividend.get배당금());
		dividend.setTax(googleSheetsDividend.get세금() == null ? BigDecimal.ZERO : googleSheetsDividend.get세금());
		dividend.setFee(BigDecimal.ZERO);
		dividend.setRecordDate(payDate);
		dividend.setPayDate(payDate);
		return dividend;
	}
	
	private static final ZoneOffset KST = ZoneOffset.ofHours(9);
	private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
			DateTimeFormatter.ofPattern("yyyy. M. d"),
			DateTimeFormatter.ofPattern("yyyy-M-d"),
			DateTimeFormatter.ISO_LOCAL_DATE);

	private Instant parsePayDate(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		var trimmed = value.trim();
		for (var formatter : DATE_FORMATTERS) {
			try {
				return LocalDate.parse(trimmed, formatter).atStartOfDay().toInstant(KST);
			} catch (DateTimeParseException ignored) {
				// try next pattern
			}
		}
		log.warn("Unable to parse dividend pay date: {}", value);
		return null;
	}
}
