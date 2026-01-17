package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.uuid.UuidGeneratorUtil;
import net.luversof.api.stock.constant.DelistedStocks;
import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockPrice;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.app.google.stock.domain.GoogleSheetDividend;
import net.luversof.app.google.stock.domain.GoogleSheetStockItem;
import net.luversof.app.google.stock.domain.GoogleSheetTrade;
import net.luversof.app.google.stock.service.StockGoogleSheetService;

@Service
@Transactional
public class StockAdminService {

	private static final Logger log = LoggerFactory.getLogger(StockAdminService.class);

	@Autowired
	private StockGoogleSheetService stockGoogleSheetService;

	@Autowired
	private StockItemRepository stockItemRepository;

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private DividendRepository dividendRepository;

	@Autowired
	private AccountService accountService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final String INSERT_STOCK_ITEM_SQL = """
			INSERT INTO "StockItem" (id, symbol, name, market)
			VALUES (?, ?, ?, ?)
			ON CONFLICT (symbol)
			DO NOTHING
			""";

	private static final String INSERT_STOCK_PRICE_SQL = """
			INSERT INTO "StockPrice" (id, "stockItem_id", price, "updatedDate")
			VALUES (?, ?, ?, ?)
			ON CONFLICT ("stockItem_id")
			DO UPDATE SET
				price = EXCLUDED.price,
				"updatedDate" = NOW()
			""";

	private static final ZoneOffset KST = ZoneOffset.ofHours(9);
	private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
			DateTimeFormatter.ofPattern("yyyy. M. d"),
			DateTimeFormatter.ofPattern("yyyy-M-d"),
			DateTimeFormatter.ISO_LOCAL_DATE);


	public int stockItemBulkInsert(UUID userId) {
		var googleSheetStockItemList = stockGoogleSheetService.getGoogleSheetStockItemList(userId);
		var allStockItems = new java.util.ArrayList<>(googleSheetStockItemList);

		for (var delistedStock : DelistedStocks.values()) {
			var googleSheetStockItem = new GoogleSheetStockItem();
			googleSheetStockItem.set종목이름(delistedStock.name());
			googleSheetStockItem.set종목코드(delistedStock.getSymbol());
			googleSheetStockItem.set현재가(BigDecimal.ZERO);
			allStockItems.add(googleSheetStockItem);
		}

		var stockItemList = allStockItems.stream().map(this::toStockItem).collect(Collectors.toList());
		
		jdbcTemplate.batchUpdate(INSERT_STOCK_ITEM_SQL, stockItemList, stockItemList.size(), (ps, item) -> {
			item.setId(UuidGeneratorUtil.getUuid());
			ps.setObject(1, item.getId());
			ps.setString(2, item.getSymbol());
			ps.setString(3, item.getName());
			ps.setString(4, item.getMarket());
		});

		var savedStockItemList = stockItemRepository.findAll();

		var stockPriceList = StreamSupport.stream(savedStockItemList.spliterator(), false)
				.map(item -> {
					var stockPrice = new StockPrice();
					stockPrice.setId(UuidGeneratorUtil.getUuid());
					stockPrice.setStockItemId(item.getId());
					stockPrice.setPrice(allStockItems.stream()
							.filter(x -> x.get종목코드().equals(item.getSymbol()))
							.findFirst()
							.map(GoogleSheetStockItem::get현재가)
							.orElse(BigDecimal.ZERO));
					stockPrice.setUpdatedDate(Instant.now());
					return stockPrice;
				})
				.toList();

		int[][] result = jdbcTemplate.batchUpdate(INSERT_STOCK_PRICE_SQL, stockPriceList, stockPriceList.size(),
				(ps, item) -> {
					ps.setObject(1, item.getId());
					ps.setObject(2, item.getStockItemId());
					ps.setObject(3, item.getPrice());
					ps.setTimestamp(4, java.sql.Timestamp.from(item.getUpdatedDate()));
				});
		
		log.debug("Processed {} stock prices", result.length);
		return result.length;
	}

	public void tradeBulkInsert(UUID userId) {
		tradeRepository.deleteAll();

		var stockItemList = StreamSupport.stream(stockItemRepository.findAll().spliterator(), false).toList();
		var googleSheetsTradeList = stockGoogleSheetService.getGoogleSheetTradeList(userId);

		var accountMap = new HashMap<String, UUID>();
		var existingAccounts = accountService.findByUserId(userId);

		existingAccounts.forEach(account -> accountMap.put(account.getName(), account.getId()));

		googleSheetsTradeList.stream()
				.map(GoogleSheetTrade::get계좌)
				.filter(accountName -> accountName != null && !accountName.isBlank())
				.distinct()
				.filter(accountName -> !accountMap.containsKey(accountName))
				.forEach(accountName -> {
					var newAccount = new Account();
					newAccount.setUserId(userId);
					newAccount.setName(accountName);
					var savedAccount = accountService.createAccount(newAccount);
					accountMap.put(accountName, savedAccount.getId());
					log.debug("Created new account: {} with id: {}", accountName, savedAccount.getId());
				});

		var tradeList = googleSheetsTradeList
				.stream()
				.map(t -> toTrade(t, accountMap, stockItemList))
				.filter(Objects::nonNull)
				.toList();
		
		log.debug("Importing {} trades", tradeList.size());
		tradeRepository.saveAll(tradeList);
	}

	public void dividendBulkInsert(UUID userId) {
		dividendRepository.deleteAll();

		var googleSheetsDividendList = stockGoogleSheetService.getGoogleSheetDividendList(userId);
		
		var accountMap = prepareAccountMap(userId, googleSheetsDividendList);
		var stockItemMap = prepareStockItemMap(googleSheetsDividendList);

		var dividends = googleSheetsDividendList.stream()
				.map(googleSheetsDividend -> toDividend(googleSheetsDividend, accountMap, stockItemMap))
				.filter(Objects::nonNull)
				.toList();

		log.debug("Importing {} dividends", dividends.size());
		dividendRepository.saveAll(dividends);
	}

	private StockItem toStockItem(GoogleSheetStockItem googleSheetStockItem) {
		StockItem stockItem = new StockItem();
		stockItem.setMarket("KRX");
		stockItem.setSymbol(googleSheetStockItem.get종목코드());
		stockItem.setName(googleSheetStockItem.get종목이름());
		return stockItem;
	}

	private Trade toTrade(GoogleSheetTrade googleSheetTrade, HashMap<String, UUID> accountMap, List<StockItem> stockItemList) {
		Trade trade = new Trade();
		trade.setType(googleSheetTrade.get구분().equals("매수") ? TradeType.BUY : TradeType.SELL);
		trade.setQuantity(googleSheetTrade.get매매_수량());
		trade.setPrice(googleSheetTrade.get매매가());
		trade.setFee(googleSheetTrade.get수수료() == null ? BigDecimal.ZERO : googleSheetTrade.get수수료());
		trade.setTax(googleSheetTrade.get거래세() == null ? BigDecimal.ZERO : googleSheetTrade.get거래세());
		trade.setTradeDate(googleSheetTrade.get날짜());
		trade.setRealizedProfit(googleSheetTrade.get매도실현손익());

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
				.orElse(null);

		if (stockItem == null) {
			log.debug("stockItem not found : {}", googleSheetTrade.get종목());
			return null;
		}

		trade.setStockItemId(stockItem.getId());
		return trade;
	}
	
	private Map<String, UUID> prepareAccountMap(UUID userId, List<GoogleSheetDividend> records) {
		var accountMap = accountService.findByUserId(userId).stream()
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
					var savedAccount = accountService.createAccount(newAccount);
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
	
	private Dividend toDividend(GoogleSheetDividend googleSheetsDividend, Map<String, UUID> accountMap, Map<String, UUID> stockItemMap) {
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
