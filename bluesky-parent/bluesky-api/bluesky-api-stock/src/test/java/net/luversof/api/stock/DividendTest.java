package net.luversof.api.stock;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.DividendCsvRecord;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.AccountRepository;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.StockItemRepository;

@Slf4j
class DividendTest implements GeneralTest {

	private static final ZoneOffset KST = ZoneOffset.ofHours(9);
	private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
			DateTimeFormatter.ofPattern("yyyy. M. d"),
			DateTimeFormatter.ofPattern("yyyy-M-d"),
			DateTimeFormatter.ISO_LOCAL_DATE);

	@Autowired
	DividendRepository dividendRepository;

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	StockItemRepository stockItemRepository;

	UUID userId = TestConstant.USER_ID;

	@Test
	void dividendBulkInsert() {
		dividendRepository.deleteAll();

		var dividendCsvRecordList = loadDividendCsvRecordList();
		assertThat(dividendCsvRecordList).isNotEmpty();

		var accountMap = prepareAccountMap(dividendCsvRecordList);
		var stockItemMap = prepareStockItemMap(dividendCsvRecordList);

		var dividends = dividendCsvRecordList.stream()
				.map(csvRecord -> toDividend(csvRecord, accountMap, stockItemMap))
				.filter(java.util.Objects::nonNull)
				.toList();

		assertThat(dividends).isNotEmpty();

		var savedDividends = StreamSupport.stream(dividendRepository.saveAll(dividends).spliterator(), false).toList();
		assertThat(savedDividends).hasSize(dividends.size());
	}

	private Map<String, UUID> prepareAccountMap(List<DividendCsvRecord> records) {
		var accountMap = accountRepository.findByUserId(userId).stream()
				.collect(Collectors.toMap(Account::getName, Account::getId, (left, right) -> left,
						java.util.LinkedHashMap::new));

		records.stream()
				.map(DividendCsvRecord::get계좌)
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

	private Map<String, UUID> prepareStockItemMap(List<DividendCsvRecord> records) {
		var stockItemMap = StreamSupport.stream(stockItemRepository.findAll().spliterator(), false)
				.collect(Collectors.toMap(StockItem::getName, StockItem::getId, (left, right) -> left,
						java.util.LinkedHashMap::new));

		records.stream()
				.map(DividendCsvRecord::get종목)
				.filter(StringUtils::hasText)
				.map(String::trim)
				.forEach(stockName -> stockItemMap.computeIfAbsent(stockName, name -> {
					var newStockItem = new StockItem();
					newStockItem.setName(name);
					newStockItem.setMarket("KOSPI");
					newStockItem.setTicker(generateTicker(name));
					var savedStockItem = stockItemRepository.save(newStockItem);
					log.debug("Created stock item for dividend import: {}", name);
					return savedStockItem.getId();
				}));

		return stockItemMap;
	}

	private String generateTicker(String baseName) {
		var alphanumeric = baseName == null ? "" : baseName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
		if (!StringUtils.hasText(alphanumeric)) {
			alphanumeric = "DIV";
		}
		var randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
		var candidate = (alphanumeric + randomSuffix);
		return candidate.substring(0, Math.min(candidate.length(), 12));
	}

	private Dividend toDividend(DividendCsvRecord csvRecord, Map<String, UUID> accountMap,
			Map<String, UUID> stockItemMap) {
		var accountName = csvRecord.get계좌();
		var stockName = csvRecord.get종목();

		if (!StringUtils.hasText(accountName) || !StringUtils.hasText(stockName)) {
			return null;
		}

		var accountId = accountMap.get(accountName.trim());
		var stockItemId = stockItemMap.get(stockName.trim());
		var payDate = parsePayDate(csvRecord.get지급일());

		if (accountId == null || stockItemId == null || payDate == null) {
			log.warn("Skip dividend row. accountId: {}, stockItemId: {}, payDate: {}", accountId, stockItemId, payDate);
			return null;
		}

		var dividend = new Dividend();
		dividend.setAccountId(accountId);
		dividend.setStockItemid(stockItemId);
		dividend.setPrice(csvRecord.get배당금() == null ? BigDecimal.ZERO : csvRecord.get배당금());
		dividend.setTax(csvRecord.get세금() == null ? BigDecimal.ZERO : csvRecord.get세금());
		dividend.setType("DIVIDEND");
		dividend.setQuantity(0);
		dividend.setFee(BigDecimal.ZERO);
		dividend.setRecordDate(payDate);
		dividend.setPayDate(payDate);
		return dividend;
	}

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

	@SneakyThrows
	List<DividendCsvRecord> loadDividendCsvRecordList() {
		var mapper = new CsvMapper();
		MappingIterator<DividendCsvRecord> iterator = mapper
				.readerFor(DividendCsvRecord.class)
				.with(CsvSchema.emptySchema().withHeader())
				.readValues(new ClassPathResource("data/divedend.csv").getInputStream());
		var records = iterator.readAll();
		log.debug("Loaded {} dividend rows", records.size());
		return records;
	}

}
