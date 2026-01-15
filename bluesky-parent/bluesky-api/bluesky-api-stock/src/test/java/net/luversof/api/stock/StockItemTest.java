package net.luversof.api.stock;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.luversof.boot.uuid.UuidGeneratorUtil;
import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockPrice;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.StockPriceRepository;
import net.luversof.api.stock.service.GoogleSheetsTestService;
import net.luversof.api.stock.service.StockItemService;
import net.luversof.api.stock.service.StockPriceService;
import net.luversof.app.google.stock.domain.GoogleSheetStockItem;
import net.luversof.app.google.stock.service.StockGoogleSheetService;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

class StockItemTest implements GeneralTest {

	private static final Logger log = LoggerFactory.getLogger(StockItemTest.class);
	
	@Autowired
	StockGoogleSheetService stockGoogleSheetService;

	@Autowired
	StockItemService stockItemService;

	@Autowired
	StockPriceService stockPriceService;

	@Autowired
	StockItemRepository stockItemRepository;

	@Autowired
	StockPriceRepository stockPriceRepository;

	@Autowired
	private GoogleSheetsTestService googleSheetsTestService;

	@Autowired
	JdbcTemplate jdbcTemplate;

	String insertStockItemSql = """
			INSERT INTO "StockItem" (id, symbol, name, market)
			VALUES (?, ?, ?, ?)
			ON CONFLICT (symbol)
			DO NOTHING
			""";

	// stockPrice에 없으면 insert 하고 있으면 update 하는 쿼리
	String insertStockPriceSql = """
			INSERT INTO "StockPrice" (id, "stockItem_id", price, "updatedDate")
			VALUES (?, ?, ?, ?)
			ON CONFLICT ("stockItem_id")
			DO UPDATE SET
				price = EXCLUDED.price,
				"updatedDate" = NOW()
			""";

	@Test
	void createStockItem() {
		var stockItem = new StockItem();
		stockItem.setSymbol("161510");
		stockItem.setName("PLUS 고배당주");
		stockItem.setMarket("KRX");

		var result = stockItemService.createStockItem(stockItem);
		log.debug("result : {}", result);
	}

	@Test
	void stockItemBulkInsert() {
		// stockItemRepository.deleteAll();
		// stockPriceRepository.deleteAll();

		var googleSheetStockItemList = stockGoogleSheetService.getGoogleSheetStockItemList(TestConstant.USER_ID);

		// 상장폐지종목 추가
		for (var delistedStock : DelistedStocks.values()) {
			var googleSheetStockItem = new GoogleSheetStockItem();
			googleSheetStockItem.set종목이름(delistedStock.name());
			googleSheetStockItem.set종목코드(delistedStock.getSymbol());
			googleSheetStockItem.set현재가(BigDecimal.ZERO);
			googleSheetStockItemList.add(googleSheetStockItem);
		}
		;

		var stockItemList = googleSheetStockItemList.stream().map(x -> toStockItem(x)).collect(Collectors.toList());
		jdbcTemplate.batchUpdate(insertStockItemSql, stockItemList, stockItemList.size(), (ps, item) -> {
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
					stockPrice.setPrice(googleSheetStockItemList.stream()
							.filter(x -> x.get종목코드().equals(item.getSymbol()))
							.findFirst()
							.map(GoogleSheetStockItem::get현재가)
							.orElse(BigDecimal.ZERO));
					stockPrice.setUpdatedDate(Instant.now());

					return stockPrice;
				})
				.toList();

		var result = jdbcTemplate.batchUpdate(insertStockPriceSql, stockPriceList, stockPriceList.size(),
				(ps, item) -> {
					ps.setObject(1, item.getId());
					ps.setObject(2, item.getStockItemId());
					ps.setObject(3, item.getPrice());
					ps.setTimestamp(4, java.sql.Timestamp.from(item.getUpdatedDate()));
				});

		log.debug("result : {}", result.length);
	}

	List<StockItem> loadSpreadSheetStockItemList() {
		List<GoogleSheetStockItem> stockItemList = googleSheetsTestService
				.getList(GoogleSheetsApiCase.GoogleSheetsStockItem);
		return stockItemList.stream().map(x -> toStockItem(x)).collect(Collectors.toList());
	}

	List<GoogleSheetStockItem> loadGoogleSheetStockItemList() {
		return googleSheetsTestService.getList(GoogleSheetsApiCase.GoogleSheetsStockItem);
	}

	List<StockItem> loadJsonStockItemList() throws StreamReadException, DatabindException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		var stockItemList = mapper.readValue(new ClassPathResource("data/stockItem.json").getInputStream(),
				new TypeReference<List<StockItem>>() {
				});

		log.debug("items : {}", stockItemList.size());
		return stockItemList;
	}

	List<StockItem> loadCsvStockItemList() throws IOException {
		var mapper = new CsvMapper();
		MappingIterator<StockItem> it = mapper
				.readerFor(StockItem.class)
				.with(CsvSchema.emptySchema().withHeader())
				.readValues(new ClassPathResource("data/stockItem.csv").getInputStream());

		var stockItemList = it.readAll();
		log.debug("items : {}", stockItemList.size());
		return stockItemList;
	}
	
	
	private StockItem toStockItem(GoogleSheetStockItem googleSheetStockItem) {
		StockItem stockItem = new StockItem();
		
		stockItem.setMarket("KRX");
		stockItem.setSymbol(googleSheetStockItem.get종목코드());
		stockItem.setName(googleSheetStockItem.get종목이름());
		return stockItem;
	}

}
