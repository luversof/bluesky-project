package net.luversof.api.stock;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.service.StockAdminService;
import net.luversof.api.stock.service.StockItemService;
import net.luversof.api.stock.service.StockPriceService;
import net.luversof.app.google.stock.domain.GoogleSheetStockItem;
import net.luversof.app.google.stock.service.StockGoogleSheetService;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

class StockItemTest implements GeneralTest {

  private static final Logger log = LoggerFactory.getLogger(StockItemTest.class);

  @Autowired StockGoogleSheetService stockGoogleSheetService;

  @Autowired StockItemService stockItemService;

  @Autowired StockPriceService stockPriceService;

  @Autowired StockItemRepository stockItemRepository;

  // StockPriceRepository removed; no longer needed in tests

  @Autowired StockAdminService stockAdminService;

  /**
   * 실사용자 데이터를 실제로 바꾸는 개발용 도구다. 자동 실행에서 돌면 안 된다. 실측 사고(2026-08-22): 프로필을 주고 AccountTest 를 돌리자
   * deleteAllByUserId 가 계좌 7 -> 0, 거래 250 -> 0, 배당 193 -> 0 으로 지웠다. 원장은 시트 재가져오기로 되돌렸지만 계좌
   * 설정(manualPrincipalAmount)은 복구 경로가 없어 잃었다. 필요할 때 이 애노테이션을 손으로 떼고 쓸 것.
   */
  @Disabled("실사용자 데이터를 바꾼다 - 필요할 때만 손으로 실행")
  @Test
  void createStockItem() {
    var stockItem = new StockItem();
    stockItem.setSymbol("161510");
    stockItem.setName("PLUS 고배당주");
    stockItem.setMarket("KRX");

    var result = stockItemService.createStockItem(stockItem);
    log.debug("result : {}", result);
  }

  /**
   * 실사용자 데이터를 실제로 바꾸는 개발용 도구다. 자동 실행에서 돌면 안 된다. 실측 사고(2026-08-22): 프로필을 주고 AccountTest 를 돌리자
   * deleteAllByUserId 가 계좌 7 -> 0, 거래 250 -> 0, 배당 193 -> 0 으로 지웠다. 원장은 시트 재가져오기로 되돌렸지만 계좌
   * 설정(manualPrincipalAmount)은 복구 경로가 없어 잃었다. 필요할 때 이 애노테이션을 손으로 떼고 쓸 것.
   */
  @Disabled("실사용자 데이터를 바꾼다 - 필요할 때만 손으로 실행")
  @Test
  void stockItemBulkInsert() {
    stockAdminService.stockItemBulkInsert(TestConstant.USER_ID);
  }

  List<StockItem> loadSpreadSheetStockItemList() {
    List<GoogleSheetStockItem> stockItemList = loadGoogleSheetStockItemList();
    return stockItemList.stream().map(x -> toStockItem(x)).collect(Collectors.toList());
  }

  List<GoogleSheetStockItem> loadGoogleSheetStockItemList() {
    return stockGoogleSheetService.getGoogleSheetStockItemList(TestConstant.USER_ID);
  }

  List<StockItem> loadJsonStockItemList()
      throws StreamReadException, DatabindException, IOException {
    JsonMapper mapper = JsonMapper.builder().build();
    var stockItemList =
        mapper.readValue(
            new ClassPathResource("data/stockItem.json").getInputStream(),
            new TypeReference<List<StockItem>>() {});

    log.debug("items : {}", stockItemList.size());
    return stockItemList;
  }

  List<StockItem> loadCsvStockItemList() throws IOException {
    var mapper = new CsvMapper();
    MappingIterator<StockItem> it =
        mapper
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
