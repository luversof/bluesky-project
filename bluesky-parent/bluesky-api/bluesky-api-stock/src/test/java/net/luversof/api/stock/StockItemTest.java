package net.luversof.api.stock;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.luversof.boot.uuid.UuidGeneratorUtil;
import net.luversof.GeneralTest;
import net.luversof.api.stock.domain.GoogleSheetsStockItem;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.service.GoogleSheetsTestService;
import net.luversof.api.stock.service.StockItemService;
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
	StockItemService stockItemService;
	
	@Autowired
	private GoogleSheetsTestService googleSheetsTestService;

	@Autowired
	JdbcTemplate jdbcTemplate;

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
		String sql = """
				INSERT INTO "StockItem" (id, symbol, name, market)
				VALUES (?, ?, ?, ?)
				ON CONFLICT (symbol)
				DO UPDATE SET
					name   = EXCLUDED.name,
					market = EXCLUDED.market
				""";

		var stockItemList = loadSpreadSheetStockItemList();

		// 상장폐지종목 추가
		for (var delistedStock : DelistedStocks.values()) {
			var stockItem = new StockItem();
			stockItem.setMarket(delistedStock.getMarket());
			stockItem.setSymbol(delistedStock.getSymbol());
			stockItem.setName(delistedStock.name());
			
			stockItemList.add(stockItem);
		};

		jdbcTemplate.batchUpdate(sql, stockItemList, stockItemList.size(), (ps, item) -> {
			item.setId(UuidGeneratorUtil.getUuid());
			ps.setObject(1, item.getId());
			ps.setString(2, item.getSymbol());
			ps.setString(3, item.getName());
			ps.setString(4, item.getMarket());
		});
	}
	
	List<StockItem> loadSpreadSheetStockItemList() {
		List<GoogleSheetsStockItem> stockItemList = googleSheetsTestService.getList(GoogleSheetsApiCase.GoogleSheetsStockItem);
		return stockItemList.stream().map(x -> x.toStockItem()).collect(Collectors.toList());
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

}
