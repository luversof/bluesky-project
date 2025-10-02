package net.luversof.api.stock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import io.github.luversof.boot.uuid.UuidGeneratorUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.service.StockItemService;

@Slf4j
class StockItemTest implements GeneralTest {
	
	@Autowired
	StockItemService stockItemService;
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	@Test
	void createStockItem() {
		var stockItem = new StockItem();
		stockItem.setTicker("161510");
		stockItem.setName("PLUS 고배당주");
		stockItem.setMarket("KOSDAQ");
		
		var result = stockItemService.createStockItem(stockItem);
		log.debug("result : {}", result);
	}
	
	
	@Test
	void stockItemBulkInsert() {
		String sql = """
				INSERT INTO "StockItem" (id, ticker, name, market)
				VALUES (?, ?, ?, ?)
				ON CONFLICT (ticker)
				DO UPDATE SET
					name   = EXCLUDED.name,
					market = EXCLUDED.market
				""";
		
		var stockItemList = loadCsvStockItemList(); 

		jdbcTemplate.batchUpdate(sql, stockItemList, stockItemList.size(), (ps, item) -> {
			item.setId(UuidGeneratorUtil.getUuid());
			ps.setObject(1, item.getId());
			ps.setString(2, item.getTicker());
			ps.setString(3, item.getName());
			ps.setString(4, item.getMarket());
		});
	}
	
	@SneakyThrows
	List<StockItem> loadJsonStockItemList() {
		ObjectMapper mapper = new ObjectMapper();
		var stockItemList = mapper.readValue(new ClassPathResource("data/stockItem.json").getInputStream(), new TypeReference<List<StockItem>>() {});
		
		log.debug("items : {}", stockItemList.size());
		return stockItemList;
	}
	
	@SneakyThrows
	List<StockItem> loadCsvStockItemList() {
		var mapper = new CsvMapper();
		MappingIterator<StockItem> it = mapper
				.readerFor(StockItem.class)
				.with(CsvSchema.emptySchema().withHeader())
				.readValues(new ClassPathResource("data/stockItem.csv").getInputStream())
				;
		
		var stockItemList = it.readAll();
		log.debug("items : {}", stockItemList.size());
		return stockItemList;
	}

}
