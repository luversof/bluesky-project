package net.luversof.web.gate.stock.httpexchange;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(
    url = "/api/stock/admin",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface StockAdminClient {

  @PostExchange("/stock-items")
  int stockItemBulkInsert(@RequestParam UUID userId);

  @PostExchange("/trades")
  net.luversof.web.gate.stock.dto.response.LedgerImportResult tradeBulkInsert(
      @RequestParam UUID userId);

  @PostExchange("/dividends")
  net.luversof.web.gate.stock.dto.response.LedgerImportResult dividendBulkInsert(
      @RequestParam UUID userId);

  /**
   * 전체 갱신을 <b>시작만</b> 하고 지금 상태를 바로 받는다(사용자 결정 2026-09-22).
   *
   * <p>53 종목에 18~22 초가 걸려 이 게이트의 읽기 제한(10 초)을 늘 넘겨 500 이 나갔다. 진행 상황은 {@link
   * #priceHistoryUpdateStatus()} 로 따로 물어본다.
   */
  @PostExchange("/price-histories")
  net.luversof.web.gate.stock.dto.response.PriceHistoryUpdateJobStatus priceHistoriesUpdate(
      @RequestParam UUID userId);

  @org.springframework.web.service.annotation.GetExchange("/price-histories/status")
  net.luversof.web.gate.stock.dto.response.PriceHistoryUpdateJobStatus priceHistoryUpdateStatus();

  /**
   * 종목 하나만 갱신한다(사용자 요청 2026-09-22).
   *
   * <p>전체 갱신은 53 종목에 18~22 초가 걸려 이 게이트의 읽기 제한(10 초)에 매번 끊겼다(실측 2026-09-22). 한 종목이면 1 초 안쪽이라 그 자리에서
   * 끝난다.
   */
  @PostExchange("/price-histories/{symbol}")
  net.luversof.web.gate.stock.dto.response.PriceHistoryUpdateResult priceHistoryUpdateOne(
      @org.springframework.web.bind.annotation.PathVariable String symbol,
      @RequestParam UUID userId);

  @PostExchange("/monthly-dividend-snapshots/import-from-sheet")
  int monthlyDividendSnapshotImportFromSheet(@RequestParam UUID userId);
}
