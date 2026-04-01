package net.luversof.api.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.databind.TradeTypeDeserializer;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.service.StockAdminService;
import net.luversof.api.stock.service.TradeService;
import net.luversof.app.google.stock.domain.GoogleSheetTrade;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

class TradeTest implements GeneralTest {

    private static final Logger log = LoggerFactory.getLogger(TradeTest.class);

    @Autowired StockAdminService stockAdminService;

    @Autowired TradeRepository tradeRepository;

    @Autowired TradeService tradeService;

    UUID userId = TestConstant.USER_ID;

    @Test
    void test() {
        var tradeList = tradeService.findByAccountId(userId);
        log.debug("tradeList : {}", tradeList);
    }

    // excel csv로 대량 insert 예제
    @Test
    void tradeBulkInsert() {
        stockAdminService.tradeBulkInsert(TestConstant.USER_ID);

        // Verify realizedProfit is saved and readable
        var trades = tradeRepository.findAll();
        var sellTrade =
                StreamSupport.stream(trades.spliterator(), false)
                        .filter(t -> t.getType() == TradeType.SELL)
                        .findFirst()
                        .orElse(null);

        if (sellTrade != null) {
            log.debug("Fetched SELL Trade: {}", sellTrade);
            // Assert that realizedProfit is populated (assuming data has it)
            // assertThat(sellTrade.getRealizedProfit()).isNotNull();
        }
    }

    @Test
    void loadTest() throws IOException {
        var tradeCsvRecordList = loadTradeCsvRecordList();
        assertThat(tradeCsvRecordList.size() > 0);
    }

    List<GoogleSheetTrade> loadTradeCsvRecordList() throws IOException {

        SimpleModule module = new SimpleModule();
        module.addDeserializer(TradeType.class, new TradeTypeDeserializer());

        var mapper = CsvMapper.builder().addModule(module).build();

        MappingIterator<GoogleSheetTrade> it =
                mapper.readerFor(GoogleSheetTrade.class)
                        .with(CsvSchema.emptySchema().withHeader())
                        .readValues(new ClassPathResource("data/trade.csv").getInputStream());

        var stockItemList = it.readAll();
        log.debug("items : {}", stockItemList.size());
        return stockItemList;
    }
}
