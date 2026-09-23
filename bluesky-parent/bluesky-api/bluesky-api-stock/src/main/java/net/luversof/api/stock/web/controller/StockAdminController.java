package net.luversof.api.stock.web.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.StockAdminService;
import net.luversof.api.stock.service.kis.KisStockPriceUpdateService;

@RestController
@RequestMapping("/api/stock/admin")
public class StockAdminController {

  @Autowired private StockAdminService stockAdminService;

  @Autowired private KisStockPriceUpdateService kisStockPriceUpdateService;

  @Autowired
  private net.luversof.api.stock.service.kis.PriceHistoryUpdateJobService
      priceHistoryUpdateJobService;

  @PostMapping("/stock-items")
  public int stockItemBulkInsert(@RequestParam UUID userId) {
    return stockAdminService.stockItemBulkInsert(userId);
  }

  /** 시트 몇 행 중 몇 행이 들어갔는지 돌려준다. void 였을 때는 버려진 행이 흔적 없이 사라졌다. */
  @PostMapping("/trades")
  public net.luversof.api.stock.web.dto.response.LedgerImportResult tradeBulkInsert(
      @RequestParam UUID userId) {
    return stockAdminService.tradeBulkInsert(userId);
  }

  @PostMapping("/dividends")
  public net.luversof.api.stock.web.dto.response.LedgerImportResult dividendBulkInsert(
      @RequestParam UUID userId) {
    return stockAdminService.dividendBulkInsert(userId);
  }

  /**
   * 전체 갱신을 <b>시작만</b> 하고 지금 상태를 바로 돌려준다(사용자 결정 2026-09-22).
   *
   * <p>53 종목에 18~22 초가 걸려 게이트의 읽기 제한(10 초)을 늘 넘겨 500 이 나갔다. 정작 여기서는 끝까지 돌아 데이터는 갱신돼 있었고 화면만 실패로
   * 보였다(실측 2026-09-22).
   *
   * <p>이미 돌고 있으면 새로 시작하지 않고 돌고 있는 상태를 그대로 돌려준다.
   */
  @PostMapping("/price-histories")
  public net.luversof.api.stock.web.dto.response.PriceHistoryUpdateJobStatus priceHistoryUpdate(
      @RequestParam UUID userId) {
    return priceHistoryUpdateJobService.start(userId);
  }

  /** 전체 갱신 작업의 지금 상태. 아무 일도 일으키지 않는다. */
  @GetMapping("/price-histories/status")
  public net.luversof.api.stock.web.dto.response.PriceHistoryUpdateJobStatus
      priceHistoryUpdateStatus() {
    return priceHistoryUpdateJobService.status();
  }

  /**
   * 종목 하나만 갱신한다(사용자 요청 2026-09-22).
   *
   * <p>전체 갱신은 53 종목에 18~22 초라 게이트 읽기 제한(10 초)에 끕겼다. 한 종목이면 1 초 안쪽이라 그 자리에서 끝난다.
   */
  @PostMapping("/price-histories/{symbol}")
  public net.luversof.api.stock.web.dto.response.PriceHistoryUpdateResult priceHistoryUpdateOne(
      @PathVariable String symbol, @RequestParam UUID userId) {
    return kisStockPriceUpdateService.updatePriceHistory(userId, symbol);
  }

  /** "배당주 검색" 시트의 보유/평단가를 월배당 기준 등록 종목에 한해 월배당 스냅샷에 추가/갱신한다. */
  @PostMapping("/monthly-dividend-snapshots/import-from-sheet")
  public int monthlyDividendSnapshotImportFromSheet(@RequestParam UUID userId) {
    return stockAdminService.importMonthlyDividendSnapshotsFromGoogleSheet(userId);
  }
}
