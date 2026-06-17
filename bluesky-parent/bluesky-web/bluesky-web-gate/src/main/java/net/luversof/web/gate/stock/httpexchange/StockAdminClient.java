package net.luversof.web.gate.stock.httpexchange;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "/api/stock/admin", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface StockAdminClient {

  @PostExchange("/stock-items")
  int stockItemBulkInsert(@RequestParam UUID userId);

  @PostExchange("/trades")
  void tradeBulkInsert(@RequestParam UUID userId);

  @PostExchange("/dividends")
  void dividendBulkInsert(@RequestParam UUID userId);

  @PostExchange("/price-histories")
  void priceHistoriesUpdate(@RequestParam UUID userId);

  @PostExchange("/monthly-dividend-snapshots/import-from-sheet")
  int monthlyDividendSnapshotImportFromSheet(@RequestParam UUID userId);
}
